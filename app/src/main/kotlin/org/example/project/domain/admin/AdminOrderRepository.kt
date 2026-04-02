package org.example.project.domain.admin

import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Products
import org.example.project.domain.character.Characters
import org.example.project.domain.character.TransactionType
import org.example.project.domain.character.Transactions
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.Order
import org.example.project.domain.order.OrderItem
import org.example.project.domain.order.OrderItems
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.Orders
import org.example.project.domain.order.SubOrder
import org.example.project.domain.order.SubOrders
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.OrderItemId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.ShippingMethodId
import org.example.project.domain.shared.SubOrderId
import org.example.project.domain.shared.TransactionId
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant

class AdminOrderRepository {

    context(_: Transaction)
    fun getMerchantOptions(): List<OrderMerchantOption> =
        Merchants.selectAll()
            .map { row ->
                OrderMerchantOption(
                    id = MerchantId(row[Merchants.id].value),
                    name = row[Merchants.name]
                )
            }
            .sortedBy { it.name.lowercase() }

    context(_: Transaction)
    fun getOrders(filter: OrderFilter): List<OrderListItem> {
        val subOrderInfoByOrderId = SubOrders.selectAll()
            .map { row ->
                SimpleSubOrderInfo(
                    orderId = OrderId(row[SubOrders.order].value),
                    merchantId = MerchantId(row[SubOrders.merchant].value),
                    status = OrderStatus.valueOf(row[SubOrders.status])
                )
            }
            .groupBy { info -> info.orderId }

        return (Orders innerJoin Characters innerJoin Currencies)
            .selectAll()
            .orderBy(Orders.createdAt to SortOrder.DESC, Orders.id to SortOrder.DESC)
            .mapNotNull { row ->
                val orderId = OrderId(row[Orders.id].value)
                val subOrders = subOrderInfoByOrderId[orderId].orEmpty()
                val status = OrderStatus.valueOf(row[Orders.status])
                val orderIdText = orderId.value.toString()
                val normalizedQuery = filter.orderIdQuery.trim().lowercase()
                if (normalizedQuery.isNotBlank() && !orderIdText.lowercase().contains(normalizedQuery)) {
                    return@mapNotNull null
                }
                if (filter.orderStatus != null && status != filter.orderStatus) {
                    return@mapNotNull null
                }
                if (filter.subOrderStatus != null && subOrders.none { info -> info.status == filter.subOrderStatus }) {
                    return@mapNotNull null
                }
                if (filter.merchantId != null && subOrders.none { info -> info.merchantId == filter.merchantId }) {
                    return@mapNotNull null
                }

                OrderListItem(
                    orderId = orderId,
                    characterName = row[Characters.name],
                    status = status,
                    merchantCount = subOrders.map { info -> info.merchantId }.distinct().size,
                    totalPrice = row[Orders.totalPrice],
                    currencyCode = row[Currencies.code],
                    createdAt = row[Orders.createdAt]
                )
            }
    }

    context(_: Transaction)
    fun getOrderDetailOrNull(orderId: OrderId): AdminOrderDetail? {
        val order = Orders.selectAll()
            .where { Orders.id eq orderId.value }
            .singleOrNull()
            ?.toOrder()
            ?: return null

        val subOrderRows = SubOrders.selectAll()
            .where { SubOrders.order eq orderId.value }
            .orderBy(SubOrders.createdAt to SortOrder.ASC, SubOrders.id to SortOrder.ASC)
            .toList()

        val subOrderIds = subOrderRows.map { SubOrderId(it[SubOrders.id].value) }
        val itemRows = if (subOrderIds.isEmpty()) emptyList()
        else OrderItems.selectAll()
            .where { OrderItems.subOrder inList subOrderIds.map { it.value } }
            .toList()
        val itemRowsBySubOrderId = itemRows
            .map { row -> row.toOrderItem() to row }
            .groupBy { (item, _) -> item.subOrderId }

        // Collect all referenced IDs
        val merchantIds = subOrderRows.map { MerchantId(it[SubOrders.merchant].value) }.toSet()
        val shippingMethodIds = subOrderRows.map { ShippingMethodId(it[SubOrders.shippingMethod].value) }.toSet()
        val productIds = itemRows.map { ProductId(it[OrderItems.product].value) }.toSet()
        val currencyIds = buildSet {
            add(order.totalCurrencyId)
            itemRows.forEach { add(CurrencyId(it[OrderItems.snapshottedCurrency].value)) }
        }

        // Batch-fetch only the referenced rows
        val characterName = Characters.selectAll()
            .where { Characters.id eq order.characterId.value }
            .singleOrNull()?.get(Characters.name) ?: "Unknown character"

        val currenciesById = if (currencyIds.isEmpty()) emptyMap()
        else Currencies.selectAll()
            .where { Currencies.id inList currencyIds.map { it.value } }
            .associateBy { row -> CurrencyId(row[Currencies.id].value) }
        val currencyCode = currenciesById[order.totalCurrencyId]?.get(Currencies.code) ?: "UNK"

        val merchantsById = if (merchantIds.isEmpty()) emptyMap()
        else Merchants.selectAll()
            .where { Merchants.id inList merchantIds.map { it.value } }
            .associateBy { row -> MerchantId(row[Merchants.id].value) }

        val shippingById = if (shippingMethodIds.isEmpty()) emptyMap()
        else ShippingMethods.selectAll()
            .where { ShippingMethods.id inList shippingMethodIds.map { it.value } }
            .associateBy { row -> ShippingMethodId(row[ShippingMethods.id].value) }

        val productsById = if (productIds.isEmpty()) emptyMap()
        else Products.selectAll()
            .where { Products.id inList productIds.map { it.value } }
            .associateBy { row -> ProductId(row[Products.id].value) }

        val subOrders = subOrderRows.map { subOrderRow ->
            val subOrder = subOrderRow.toSubOrder()
            val merchantRow = merchantsById[subOrder.merchantId]
            val shippingRow = shippingById[subOrder.shippingMethodId]
            val itemDetails = itemRowsBySubOrderId[subOrder.id].orEmpty()
                .map { (item, _) ->
                    val productRow = productsById[item.productId]
                    val itemCurrencyCode = currenciesById[item.snapshottedCurrencyId]?.get(Currencies.code) ?: currencyCode
                    AdminOrderItemDetail(
                        item = item,
                        productName = productRow?.get(Products.name) ?: "Unknown product",
                        productCategory = productRow?.get(Products.category) ?: "Unknown category",
                        productDescription = productRow?.get(Products.description),
                        merchantName = merchantRow?.get(Merchants.name) ?: "Unknown merchant",
                        currencyCode = itemCurrencyCode,
                        unitPrice = item.snapshottedPrice,
                        subtotal = item.snapshottedPrice * item.quantity.toLong()
                    )
                }

            AdminSubOrderDetail(
                subOrder = subOrder,
                merchantName = merchantRow?.get(Merchants.name) ?: "Unknown merchant",
                shippingMethodName = shippingRow?.get(ShippingMethods.name) ?: "Unknown shipping method",
                shippingCostCurrencyCode = shippingRow?.let { row ->
                    currenciesById[CurrencyId(row[ShippingMethods.currency].value)]?.get(Currencies.code)
                } ?: currencyCode,
                items = itemDetails
            )
        }

        return AdminOrderDetail(
            order = order,
            characterName = characterName,
            currencyCode = currencyCode,
            subOrders = subOrders,
            history = buildOrderHistoryEvents(
                orderId = order.id,
                orderStatus = order.status.name,
                orderCreatedAt = order.createdAt,
                orderUpdatedAt = order.updatedAt,
                currencyCode = currencyCode,
                subOrders = subOrders,
                transactions = getOrderTransactions(orderId)
            )
        )
    }

    context(_: Transaction)
    private fun getOrderTransactions(orderId: OrderId): List<org.example.project.domain.character.Transaction> =
        Transactions.selectAll()
            .where {
                (Transactions.referenceType eq "ORDER") and (Transactions.referenceId eq orderId.value)
            }
            .orderBy(Transactions.createdAt to SortOrder.ASC, Transactions.id to SortOrder.ASC)
            .map { row ->
                org.example.project.domain.character.Transaction(
                    id = TransactionId(row[Transactions.id].value),
                    characterId = CharacterId(row[Transactions.character].value),
                    currencyId = CurrencyId(row[Transactions.currency].value),
                    amount = row[Transactions.amount],
                    type = TransactionType.valueOf(row[Transactions.type]),
                    referenceId = row[Transactions.referenceId],
                    referenceType = row[Transactions.referenceType],
                    description = row[Transactions.description],
                    createdAt = row[Transactions.createdAt],
                    updatedAt = row[Transactions.updatedAt]
                )
            }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toOrder(): Order =
        Order(
            id = OrderId(this[Orders.id].value),
            characterId = CharacterId(this[Orders.character].value),
            status = OrderStatus.valueOf(this[Orders.status]),
            totalPrice = this[Orders.totalPrice],
            totalCurrencyId = CurrencyId(this[Orders.totalCurrency].value),
            createdAt = this[Orders.createdAt],
            updatedAt = this[Orders.updatedAt]
        )

    private fun org.jetbrains.exposed.v1.core.ResultRow.toSubOrder(): SubOrder =
        SubOrder(
            id = SubOrderId(this[SubOrders.id].value),
            orderId = OrderId(this[SubOrders.order].value),
            merchantId = MerchantId(this[SubOrders.merchant].value),
            status = OrderStatus.valueOf(this[SubOrders.status]),
            shippingMethodId = ShippingMethodId(this[SubOrders.shippingMethod].value),
            shippingCost = this[SubOrders.shippingCost],
            merchantTotalPrice = this[SubOrders.merchantTotalPrice],
            createdAt = this[SubOrders.createdAt],
            updatedAt = this[SubOrders.updatedAt]
        )

    private fun org.jetbrains.exposed.v1.core.ResultRow.toOrderItem(): OrderItem =
        OrderItem(
            id = OrderItemId(this[OrderItems.id].value),
            subOrderId = SubOrderId(this[OrderItems.subOrder].value),
            productId = ProductId(this[OrderItems.product].value),
            quantity = this[OrderItems.quantity],
            snapshottedPrice = this[OrderItems.snapshottedPrice],
            snapshottedCurrencyId = CurrencyId(this[OrderItems.snapshottedCurrency].value),
            createdAt = this[OrderItems.createdAt],
            updatedAt = this[OrderItems.updatedAt]
        )

    private data class SimpleSubOrderInfo(
        val orderId: OrderId,
        val merchantId: MerchantId,
        val status: OrderStatus
    )
}
