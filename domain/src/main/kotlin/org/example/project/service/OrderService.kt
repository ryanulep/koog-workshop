package org.example.project.service

import org.example.project.db.repository.*
import org.example.project.db.suspendTransaction
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.id.*
import org.example.project.domain.model.*
import org.jetbrains.exposed.v1.jdbc.Database

class OrderService(
    private val database: Database,
    private val orderRepository: OrderRepository = OrderRepository(),
    private val cartRepository: CartRepository = CartRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
    private val characterRepository: CharacterRepository = CharacterRepository(),
    private val shippingRepository: ShippingRepository = ShippingRepository()
) {
    /**
     * Checkout converts the character's cart into an order.
     *
     * [shippingSelections] maps each merchant to the chosen shipping method.
     * All operations happen within a single transaction:
     * - Validates stock for every cart item
     * - Groups items by merchant, creates Order -> SubOrders -> OrderItems
     * - Deducts wallet balance (PURCHASE transactions)
     * - Decrements product stock
     * - Clears cart
     */
    suspend fun checkout(
        characterId: CharacterId,
        shippingSelections: Map<MerchantId, ShippingMethodId>
    ): OrderId = database.suspendTransaction {
        val cartItems = cartRepository.getCartItems(characterId)
        require(cartItems.isNotEmpty()) { "Cart is empty" }

        // Load all products and validate stock
        val products = cartItems.map { cartItem ->
            val product = productRepository.getProductOrNull(cartItem.productId)
                ?: throw IllegalStateException("Product not found: ${cartItem.productId}")
            require(product.isActive) { "Product is not active: ${product.name}" }
            require(product.stock >= cartItem.quantity) {
                "Insufficient stock for ${product.name}: requested ${cartItem.quantity}, available ${product.stock}"
            }
            cartItem to product
        }

        // Group by merchant
        val byMerchant = products.groupBy { (_, product) -> product.merchantId }

        // All items must share the same currency for total calculation
        val currencyId = products.first().second.currencyId

        // Calculate total across all merchants including shipping
        var grandTotal = 0L
        val merchantTotals = byMerchant.map { (merchantId, items) ->
            val shippingMethodId = shippingSelections[merchantId]
                ?: throw IllegalArgumentException("No shipping method selected for merchant: $merchantId")
            val shippingMethod = shippingRepository.getShippingMethodByIdOrNull(shippingMethodId)
                ?: throw IllegalArgumentException("Shipping method not found: $shippingMethodId")
            val productTotal = items.sumOf { (cartItem, product) -> product.price * cartItem.quantity }
            val merchantTotal = productTotal + shippingMethod.baseCost
            grandTotal += merchantTotal
            MerchantCheckout(merchantId, shippingMethodId, shippingMethod.baseCost, merchantTotal, items)
        }

        // Validate wallet balance
        val balance = characterRepository.getWalletBalance(characterId)
        val currentBalance = balance[currencyId] ?: 0L
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

        orderId
    }

    suspend fun getOrder(id: OrderId): Order? =
        database.suspendTransaction { orderRepository.getOrderOrNull(id) }

    suspend fun getOrderHistory(
        characterId: CharacterId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Order> =
        database.suspendTransaction { orderRepository.getOrderHistory(characterId, offset, limit) }

    suspend fun getOrderDetailsOrNull(orderId: OrderId): OrderDetails? =
        database.suspendTransaction {
            val order = orderRepository.getOrderOrNull(orderId) ?: return@suspendTransaction null
            val subOrders = orderRepository.getSubOrders(orderId).map { subOrder ->
                SubOrderDetails(
                    subOrder = subOrder,
                    items = orderRepository.getOrderItems(subOrder.id)
                )
            }
            OrderDetails(order, subOrders)
        }

    suspend fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean =
        database.suspendTransaction { orderRepository.updateSubOrderStatus(subOrderId, status) }

    /**
     * Cancels a PENDING order: refunds wallet, restores stock, updates status.
     */
    suspend fun cancelOrder(orderId: OrderId): Boolean = database.suspendTransaction {
        val order = orderRepository.getOrderOrNull(orderId) ?: return@suspendTransaction false
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

        orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    private data class MerchantCheckout(
        val merchantId: MerchantId,
        val shippingMethodId: ShippingMethodId,
        val shippingCost: Long,
        val merchantTotal: Long,
        val items: List<Pair<CartItem, Product>>
    )
}
