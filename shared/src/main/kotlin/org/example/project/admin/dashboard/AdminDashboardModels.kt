package org.example.project.admin.dashboard

import kotlinx.serialization.Serializable
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.OrderId

@Serializable
data class RecentOrderSummary(
    val orderId: OrderId,
    val status: OrderStatus,
    val characterName: String,
    val totalPrice: Long,
    val totalCurrencyCode: String,
    val createdAt: kotlin.time.Instant
)
