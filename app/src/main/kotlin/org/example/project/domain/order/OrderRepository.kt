package org.example.project.domain.order

import org.example.project.domain.character.Characters
import org.example.project.domain.character.Transaction as OrderTransaction
import org.example.project.domain.character.TransactionType
import org.example.project.domain.character.Transactions
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Products
import org.example.project.domain.currency.Currencies
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.*
import org.example.project.domain.shared.Page
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.example.project.db.update as storeUpdate

class OrderRepository {

    context(_: Transaction)
    fun getOrderOrNull(id: OrderId): Order? =
        Orders.selectAll().where { Orders.id eq id.value }
            .map(::mapToOrder)
            .singleOrNull()

    context(_: Transaction)
    fun getSubOrderOrNull(id: SubOrderId): SubOrder? =
        SubOrders.selectAll().where { SubOrders.id eq id.value }
            .map(::mapToSubOrder)
            .singleOrNull()

    context(_: Transaction)
    fun getOrderItemOrNull(id: OrderItemId): OrderItem? =
        OrderItems.selectAll().where { OrderItems.id eq id.value }
            .map(::mapToOrderItem)
            .singleOrNull()

    context(_: Transaction)
    fun getOrderHistory(characterId: CharacterId, offset: Long, limit: Long): Page<Order> {
        val query = Orders.selectAll().where { Orders.character eq characterId.value }
            .orderBy(Orders.createdAt, SortOrder.DESC)
        val items = query.copy()
            .limit(limit.toInt()).offset(offset)
            .map(::mapToOrder)
        val total = query.count()
        return Page(items, total, offset, limit)
    }

    context(_: Transaction)
    fun getOrderHistory(characterId: CharacterId): List<Order> =
        Orders.selectAll().where { Orders.character eq characterId.value }
            .orderBy(Orders.createdAt, SortOrder.DESC)
            .map(::mapToOrder)

    context(_: Transaction)
    fun getSubOrders(orderId: OrderId): List<SubOrder> =
        SubOrders.selectAll().where { SubOrders.order eq orderId.value }
            .map(::mapToSubOrder)

    context(_: Transaction)
    fun getOrderItems(subOrderId: SubOrderId): List<OrderItem> =
        OrderItems.selectAll().where { OrderItems.subOrder eq subOrderId.value }
            .map(::mapToOrderItem)

    context(_: Transaction)
    fun getOrderTransactions(orderId: OrderId): List<OrderTransaction> =
        Transactions.selectAll()
            .where {
                (Transactions.referenceType eq "ORDER") and (Transactions.referenceId eq orderId.value)
            }
            .orderBy(Transactions.createdAt to SortOrder.ASC, Transactions.id to SortOrder.ASC)
            .map(::mapToTransaction)

    context(_: Transaction)
    fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean =
        SubOrders.storeUpdate({ SubOrders.id eq subOrderId.value }) {
            it[SubOrders.status] = status.name
        } > 0

    context(_: Transaction)
    fun createOrder(characterId: CharacterId, totalPrice: Long, totalCurrencyId: CurrencyId): OrderId {
        require(totalPrice >= 0) { "Order total must be non-negative" }
        return OrderId(
            Orders.insertAndGetId {
                it[character] = characterId.value
                it[status] = OrderStatus.PENDING.name
                it[Orders.totalPrice] = totalPrice
                it[totalCurrency] = totalCurrencyId.value
            }.value
        )
    }

    context(_: Transaction)
    fun createSubOrder(
        orderId: OrderId,
        merchantId: MerchantId,
        shippingMethodId: ShippingMethodId,
        shippingCost: Long,
        merchantTotalPrice: Long
    ): SubOrderId {
        require(shippingCost >= 0) { "Shipping cost must be non-negative" }
        require(merchantTotalPrice >= 0) { "Merchant total must be non-negative" }
        return SubOrderId(
            SubOrders.insertAndGetId {
                it[order] = orderId.value
                it[merchant] = merchantId.value
                it[status] = OrderStatus.PENDING.name
                it[shippingMethod] = shippingMethodId.value
                it[SubOrders.shippingCost] = shippingCost
                it[SubOrders.merchantTotalPrice] = merchantTotalPrice
            }.value
        )
    }

    context(_: Transaction)
    fun createOrderItem(
        subOrderId: SubOrderId,
        productId: ProductId,
        quantity: Int,
        snapshottedPrice: Long,
        snapshottedCurrencyId: CurrencyId
    ): OrderItemId {
        require(quantity > 0) { "Order item quantity must be positive" }
        require(snapshottedPrice >= 0) { "Snapshotted price must be non-negative" }
        return OrderItemId(
            OrderItems.insertAndGetId {
                it[subOrder] = subOrderId.value
                it[product] = productId.value
                it[OrderItems.quantity] = quantity
                it[OrderItems.snapshottedPrice] = snapshottedPrice
                it[snapshottedCurrency] = snapshottedCurrencyId.value
            }.value
        )
    }

    context(_: Transaction)
    fun updateOrderStatus(orderId: OrderId, status: OrderStatus): Boolean =
        Orders.storeUpdate({ Orders.id eq orderId.value }) {
            it[Orders.status] = status.name
        } > 0

    context(_: Transaction)
    fun deleteOrder(orderId: OrderId): Boolean {
        val subOrderIds = SubOrders.selectAll().where { SubOrders.order eq orderId.value }
            .map { it[SubOrders.id].value }
        for (subOrderId in subOrderIds) {
            OrderItems.deleteWhere { OrderItems.subOrder eq subOrderId }
        }
        SubOrders.deleteWhere { SubOrders.order eq orderId.value }
        return Orders.deleteWhere { Orders.id eq orderId.value } > 0
    }

    private fun mapToOrder(row: ResultRow) = Order(
        id = OrderId(row[Orders.id].value),
        characterId = CharacterId(row[Orders.character].value),
        status = OrderStatus.valueOf(row[Orders.status]),
        totalPrice = row[Orders.totalPrice],
        totalCurrencyId = CurrencyId(row[Orders.totalCurrency].value),
        createdAt = row[Orders.createdAt],
        updatedAt = row[Orders.updatedAt]
    )

    private fun mapToSubOrder(row: ResultRow) = SubOrder(
        id = SubOrderId(row[SubOrders.id].value),
        orderId = OrderId(row[SubOrders.order].value),
        merchantId = MerchantId(row[SubOrders.merchant].value),
        status = OrderStatus.valueOf(row[SubOrders.status]),
        shippingMethodId = ShippingMethodId(row[SubOrders.shippingMethod].value),
        shippingCost = row[SubOrders.shippingCost],
        merchantTotalPrice = row[SubOrders.merchantTotalPrice],
        createdAt = row[SubOrders.createdAt],
        updatedAt = row[SubOrders.updatedAt]
    )

    private fun mapToOrderItem(row: ResultRow) = OrderItem(
        id = OrderItemId(row[OrderItems.id].value),
        subOrderId = SubOrderId(row[OrderItems.subOrder].value),
        productId = ProductId(row[OrderItems.product].value),
        quantity = row[OrderItems.quantity],
        snapshottedPrice = row[OrderItems.snapshottedPrice],
        snapshottedCurrencyId = CurrencyId(row[OrderItems.snapshottedCurrency].value),
        createdAt = row[OrderItems.createdAt],
        updatedAt = row[OrderItems.updatedAt]
    )

    private fun mapToTransaction(row: ResultRow) = OrderTransaction(
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
