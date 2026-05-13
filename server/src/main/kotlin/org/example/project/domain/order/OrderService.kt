package org.example.project.domain.order

import org.example.project.domain.cart.CartRepository
import org.example.project.domain.cart.CartItem
import org.example.project.domain.catalog.Product
import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.character.CharacterRepository
import org.example.project.domain.character.TransactionType
import org.example.project.domain.currency.CurrencyRepository
import org.example.project.domain.shipping.ShippingRepository
import org.example.project.domain.shipping.ShippingMethod

import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToLong

@Component
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val characterRepository: CharacterRepository,
    private val shippingRepository: ShippingRepository,
    private val currencyRepository: CurrencyRepository
) {
    /**
     * Checkout converts the character's cart into an order.
     *
     * [shippingSelections] maps each merchant to the chosen shipping method.
     * All operations happen within a single transaction:
     * - Validates stock for every cart item
     * - Groups items by merchant, creates Order -> SubOrders -> OrderItems
     * - Normalizes all prices into a deterministic settlement currency
     * - Deducts wallet balance (PURCHASE transactions)
     * - Decrements product stock
     * - Clears cart
     */
    suspend fun checkout(
        characterId: CharacterId,
        shippingSelections: Map<MerchantId, ShippingMethodId>
    ): OrderId {
        val cartItems = cartRepository.getCartItems(characterId)
        require(cartItems.isNotEmpty()) { "Cart is empty" }

        // Load all products and validate stock
        val products = cartItems.map { cartItem ->
            val product = productRepository.getProductOrNull(cartItem.productId)
                ?: throw IllegalStateException("Product not found: ${cartItem.productId}")
            require(cartItem.quantity > 0) { "Cart item quantity must be positive: ${cartItem.productId}" }
            require(product.isActive) { "Product is not active: ${product.name}" }
            require(product.price >= 0) { "Product price must be non-negative: ${product.name}" }
            require(product.stock >= cartItem.quantity) {
                "Insufficient stock for ${product.name}: requested ${cartItem.quantity}, available ${product.stock}"
            }
            cartItem to product
        }

        // Group by merchant
        val byMerchant = products.groupBy { (_, product) -> product.merchantId }
        val merchantShippingMethods = byMerchant.keys.associateWith { merchantId ->
            val shippingMethodId = shippingSelections[merchantId]
                ?: throw IllegalArgumentException("No shipping method selected for merchant: $merchantId")
            shippingRepository.getShippingMethodsForMerchant(merchantId)
                .firstOrNull { it.id == shippingMethodId }
                ?: throw IllegalArgumentException(
                    "Shipping method $shippingMethodId is not available for merchant: $merchantId"
                )
        }
        merchantShippingMethods.values.forEach { shippingMethod ->
            require(shippingMethod.isActive) {
                "Shipping method is not active: ${shippingMethod.name}"
            }
            require(shippingMethod.baseCost >= 0) {
                "Shipping base cost must be non-negative: ${shippingMethod.name}"
            }
        }

        val walletBalances = characterRepository.getWalletBalance(characterId)
        val currencyId = resolveSettlementCurrencyId(
            products = products,
            shippingMethods = merchantShippingMethods.values,
            walletBalances = walletBalances
        )

        // Calculate total across all merchants including shipping
        var grandTotal = 0L
        val merchantTotals = byMerchant.map { (merchantId, items) ->
            val shippingMethod = merchantShippingMethods.getValue(merchantId)
            val productTotal = items.sumOf { (cartItem, product) ->
                convertAmount(
                    amount = product.price * cartItem.quantity.toLong(),
                    fromCurrencyId = product.currencyId,
                    toCurrencyId = currencyId
                )
            }
            val shippingCost = convertAmount(
                amount = shippingMethod.baseCost,
                fromCurrencyId = shippingMethod.currencyId,
                toCurrencyId = currencyId
            )
            val merchantTotal = productTotal + shippingCost
            grandTotal += merchantTotal
            MerchantCheckout(merchantId, shippingMethod.id, shippingCost, merchantTotal, items)
        }

        // Validate wallet balance
        val currentBalance = walletBalances[currencyId] ?: 0L
        require(currentBalance >= grandTotal) {
            "Insufficient wallet balance: required $grandTotal, available $currentBalance"
        }

        // Create the order
        val orderId = orderRepository.createOrder(characterId, grandTotal, currencyId)

        for (mc in merchantTotals) {
            val subOrderId = orderRepository.createSubOrder(
                orderId = orderId,
                merchantId = mc.merchantId,
                shippingMethodId = mc.shippingMethodId,
                shippingCost = mc.shippingCost,
                merchantTotalPrice = mc.merchantTotal
            )
            for ((cartItem, product) in mc.items) {
                orderRepository.createOrderItem(
                    subOrderId = subOrderId,
                    productId = product.id,
                    quantity = cartItem.quantity,
                    snapshottedPrice = product.price,
                    snapshottedCurrencyId = product.currencyId
                )
                // Decrement stock
                productRepository.updateStock(product.id, -cartItem.quantity)
            }
        }

        // Deduct wallet
        characterRepository.addTransaction(
            characterId = characterId,
            currencyId = currencyId,
            amount = -grandTotal,
            type = TransactionType.PURCHASE,
            referenceId = orderId.value,
            referenceType = "ORDER",
            description = "Purchase order ${orderId.value}"
        )

        // Clear cart
        cartRepository.clearCart(characterId)

        return orderId
    }

    fun getOrderOrNull(id: OrderId): Order? =
        orderRepository.getOrderOrNull(id)

    fun getOrderHistory(
        characterId: CharacterId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Order> =
        orderRepository.getOrderHistory(characterId, offset, limit)

    fun getOrderDetailsOrNull(orderId: OrderId): OrderDetails? {
        val order = orderRepository.getOrderOrNull(orderId) ?: return null
        val subOrders = orderRepository.getSubOrders(orderId).map { subOrder ->
            SubOrderDetails(
                subOrder = subOrder,
                items = orderRepository.getOrderItems(subOrder.id)
            )
        }
        return OrderDetails(order, subOrders)
    }


    fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean =
        orderRepository.updateSubOrderStatus(subOrderId, status)

    /**
     * Cancels a PENDING order: refunds wallet, restores stock, updates status.
     */
    fun cancelOrder(orderId: OrderId): Boolean {
        val order = orderRepository.getOrderOrNull(orderId) ?: return false
        require(order.status == OrderStatus.PENDING) {
            "Can only cancel PENDING orders, current status: ${order.status}"
        }

        // Restore stock for all items
        val subOrders = orderRepository.getSubOrders(orderId)
        for (subOrder in subOrders) {
            val items = orderRepository.getOrderItems(subOrder.id)
            for (item in items) {
                productRepository.updateStock(item.productId, item.quantity)
            }
            orderRepository.updateSubOrderStatus(subOrder.id, OrderStatus.CANCELLED)
        }

        // Refund wallet
        characterRepository.addTransaction(
            characterId = order.characterId,
            currencyId = order.totalCurrencyId,
            amount = order.totalPrice,
            type = TransactionType.REFUND,
            referenceId = orderId.value,
            referenceType = "ORDER",
            description = "Refund for cancelled order ${orderId.value}"
        )

        return orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    private data class MerchantCheckout(
        val merchantId: MerchantId,
        val shippingMethodId: ShippingMethodId,
        val shippingCost: Long,
        val merchantTotal: Long,
        val items: List<Pair<CartItem, Product>>
    )

    
    private fun resolveSettlementCurrencyId(
        products: List<Pair<CartItem, Product>>,
        shippingMethods: Collection<ShippingMethod>,
        walletBalances: Map<CurrencyId, Long>
    ): CurrencyId {
        val sourceCurrencies = buildSet {
            products.forEach { add(it.second.currencyId) }
            shippingMethods.forEach { add(it.currencyId) }
        }.toList()

        val goldCurrencyId = currencyRepository.getAllCurrencies()
            .firstOrNull { it.code == "GOLD" }
            ?.id

        if (goldCurrencyId != null &&
            (walletBalances[goldCurrencyId] ?: 0L) > 0 &&
            sourceCurrencies.all { canConvertBetween(it, goldCurrencyId) }
        ) {
            return goldCurrencyId
        }

        val convertibleWalletCurrencies = walletBalances.entries
            .asSequence()
            .filter { it.value > 0 }
            .filter { entry -> sourceCurrencies.all { canConvertBetween(it, entry.key) } }
            .sortedWith(
                compareByDescending<Map.Entry<CurrencyId, Long>> { it.value }
                    .thenBy { it.key.value.toString() }
            )
            .map { it.key }
            .toList()

        if (convertibleWalletCurrencies.isNotEmpty()) {
            return convertibleWalletCurrencies.first()
        }

        if (goldCurrencyId != null && (walletBalances[goldCurrencyId] ?: 0L) > 0) {
            return goldCurrencyId
        }

        return walletBalances.entries
            .filter { it.value > 0 }
            .maxWithOrNull(
                compareBy<Map.Entry<CurrencyId, Long>> { it.value }
                    .thenBy { it.key.value.toString() }
            )
            ?.key
            ?: sourceCurrencies.sortedBy { it.value.toString() }.first()
    }

    
    private fun canConvertBetween(fromCurrencyId: CurrencyId, toCurrencyId: CurrencyId): Boolean =
        fromCurrencyId == toCurrencyId ||
                currencyRepository.getConversionRateOrNull(fromCurrencyId, toCurrencyId) != null ||
                currencyRepository.getConversionRateOrNull(toCurrencyId, fromCurrencyId) != null

    
    private fun convertAmount(
        amount: Long,
        fromCurrencyId: CurrencyId,
        toCurrencyId: CurrencyId
    ): Long {
        if (fromCurrencyId == toCurrencyId) return amount

        val directRate = currencyRepository.getConversionRateOrNull(fromCurrencyId, toCurrencyId)
        if (directRate != null) {
            return (amount.toDouble() * directRate).roundToLong()
        }

        val inverseRate = currencyRepository.getConversionRateOrNull(toCurrencyId, fromCurrencyId)
            ?: throw IllegalArgumentException(
                "No conversion rate available between $fromCurrencyId and $toCurrencyId"
            )
        return (amount.toDouble() / inverseRate).roundToLong()
    }
}
