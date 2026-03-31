package org.example.project.db.repository

import org.example.project.db.DatabaseFactory.dbQuery
import org.example.project.db.tables.*
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.model.Order
import org.example.project.domain.model.SubOrder
import org.example.project.domain.model.OrderItem
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class OrderRepository {

    suspend fun getOrderById(id: Long): Order? = dbQuery {
        Orders.selectAll().where { Orders.id eq id }
            .map(::mapToOrder)
            .singleOrNull()
    }

    suspend fun getOrderHistory(characterId: Long): List<Order> = dbQuery {
        Orders.selectAll().where { Orders.character eq characterId }
            .orderBy(Orders.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
            .map(::mapToOrder)
    }

    suspend fun getSubOrders(orderId: Long): List<SubOrder> = dbQuery {
        SubOrders.selectAll().where { SubOrders.order eq orderId }
            .map(::mapToSubOrder)
    }

    suspend fun getOrderItems(subOrderId: Long): List<OrderItem> = dbQuery {
        OrderItems.selectAll().where { OrderItems.subOrder eq subOrderId }
            .map(::mapToOrderItem)
    }

    suspend fun updateSubOrderStatus(subOrderId: Long, status: OrderStatus): Boolean = dbQuery {
        SubOrders.update({ SubOrders.id eq subOrderId }) {
            it[SubOrders.status] = status.name
            it[SubOrders.updatedAt] = System.currentTimeMillis()
        } > 0
    }

    private fun mapToOrder(row: ResultRow) = Order(
        id = row[Orders.id].value,
        characterId = row[Orders.character].value,
        status = OrderStatus.valueOf(row[Orders.status]),
        totalPrice = row[Orders.totalPrice],
        totalCurrencyId = row[Orders.totalCurrency].value,
        createdAt = row[Orders.createdAt],
        updatedAt = row[Orders.updatedAt]
    )

    private fun mapToSubOrder(row: ResultRow) = SubOrder(
        id = row[SubOrders.id].value,
        orderId = row[SubOrders.order].value,
        merchantId = row[SubOrders.merchant].value,
        status = OrderStatus.valueOf(row[SubOrders.status]),
        shippingMethodId = row[SubOrders.shippingMethod].value,
        shippingCost = row[SubOrders.shippingCost],
        merchantTotalPrice = row[SubOrders.merchantTotalPrice],
        createdAt = row[SubOrders.createdAt],
        updatedAt = row[SubOrders.updatedAt]
    )

    private fun mapToOrderItem(row: ResultRow) = OrderItem(
        id = row[OrderItems.id].value,
        subOrderId = row[OrderItems.subOrder].value,
        productId = row[OrderItems.product].value,
        quantity = row[OrderItems.quantity],
        snapshottedPrice = row[OrderItems.snapshottedPrice],
        snapshottedCurrencyId = row[OrderItems.snapshottedCurrency].value,
        createdAt = row[OrderItems.createdAt]
    )
}
