package org.example.project.db.repository

import org.example.project.db.tables.*
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.id.*
import org.example.project.domain.model.Order
import org.example.project.domain.model.OrderItem
import org.example.project.domain.model.SubOrder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExposedOrderRepository {

    context(_: Transaction)
    fun getOrderOrNull(id: OrderId): Order? =
        Orders.selectAll().where { Orders.id eq id.value }
            .map(::mapToOrder)
            .singleOrNull()

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
    fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean =
        SubOrders.update({ SubOrders.id eq subOrderId.value }) {
            it[SubOrders.status] = status.name
        } > 0

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
}
