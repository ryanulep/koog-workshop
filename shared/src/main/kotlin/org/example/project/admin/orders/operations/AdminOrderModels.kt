package org.example.project.admin.orders.operations

import kotlinx.serialization.Serializable
import org.example.project.domain.character.Transaction
import org.example.project.domain.order.Order
import org.example.project.domain.order.OrderItem
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.SubOrder
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import kotlin.math.abs
import kotlin.time.Instant

@Serializable
data class OrderFilter(
    val orderIdQuery: String = "",
    val orderStatus: OrderStatus? = null,
    val subOrderStatus: OrderStatus? = null,
    val merchantId: MerchantId? = null
)

@Serializable
data class OrderMerchantOption(
    val id: MerchantId,
    val name: String
)

@Serializable
data class OrderListItem(
    val orderId: OrderId,
    val characterName: String,
    val status: OrderStatus,
    val merchantCount: Int,
    val totalPrice: Long,
    val currencyCode: String,
    val createdAt: Instant
)

@Serializable
data class AdminOrderDetail(
    val order: Order,
    val characterName: String,
    val currencyCode: String,
    val subOrders: List<AdminSubOrderDetail>,
    val history: List<AdminOrderHistoryEvent>
)

@Serializable
data class AdminSubOrderDetail(
    val subOrder: SubOrder,
    val merchantName: String,
    val shippingMethodName: String,
    val shippingCostCurrencyCode: String,
    val items: List<AdminOrderItemDetail>
)

@Serializable
data class AdminOrderItemDetail(
    val item: OrderItem,
    val productName: String,
    val productCategory: String,
    val productDescription: String?,
    val merchantName: String,
    val currencyCode: String,
    val unitPrice: Long,
    val subtotal: Long
)

@Serializable
data class AdminOrderHistoryEvent(
    val timestamp: Instant,
    val title: String,
    val description: String
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: OrderStatus
)

fun buildOrderHistoryEvents(
    orderId: OrderId,
    orderStatus: String,
    orderCreatedAt: Instant,
    orderUpdatedAt: Instant,
    currencyCode: String,
    subOrders: List<AdminSubOrderDetail>,
    transactions: List<Transaction>
): List<AdminOrderHistoryEvent> {
    data class TimelineEntry(
        val timestamp: Instant,
        val priority: Int,
        val title: String,
        val description: String
    )

    val entries = buildList {
        add(
            TimelineEntry(
                timestamp = orderCreatedAt,
                priority = 0,
                title = "Order created",
                description = "Order ${orderId.value} was created."
            )
        )

        if (orderUpdatedAt != orderCreatedAt) {
            add(
                TimelineEntry(
                    timestamp = orderUpdatedAt,
                    priority = 4,
                    title = "Order updated",
                    description = "Order ${orderId.value} is now $orderStatus."
                )
            )
        }

        subOrders.forEach { detail ->
            val subOrder = detail.subOrder
            add(
                TimelineEntry(
                    timestamp = subOrder.createdAt,
                    priority = 1,
                    title = "Sub-order created",
                    description = "${detail.merchantName} received sub-order ${subOrder.id.value}."
                )
            )

            if (subOrder.updatedAt != subOrder.createdAt) {
                add(
                    TimelineEntry(
                        timestamp = subOrder.updatedAt,
                        priority = 3,
                        title = "Sub-order updated",
                        description = "${detail.merchantName} updated sub-order ${subOrder.id.value} to ${subOrder.status.name}."
                    )
                )
            }
        }

        transactions.forEach { transaction ->
            val title = when (transaction.type) {
                org.example.project.domain.character.TransactionType.PURCHASE -> "Purchase recorded"
                org.example.project.domain.character.TransactionType.REFUND -> "Refund recorded"
                org.example.project.domain.character.TransactionType.DEPOSIT -> "Deposit recorded"
                org.example.project.domain.character.TransactionType.EXCHANGE_DEBIT -> "Exchange debit recorded"
                org.example.project.domain.character.TransactionType.EXCHANGE_CREDIT -> "Exchange credit recorded"
            }
            val description = transaction.description
                ?: "${transaction.type.name.lowercase().replace('_', ' ')} of ${abs(transaction.amount)} $currencyCode."

            add(
                TimelineEntry(
                    timestamp = transaction.createdAt,
                    priority = 2,
                    title = title,
                    description = description
                )
            )
        }
    }

    return entries
        .sortedWith(compareBy<TimelineEntry> { it.timestamp }.thenBy { it.priority }.thenBy { it.title })
        .map { AdminOrderHistoryEvent(it.timestamp, it.title, it.description) }
}
