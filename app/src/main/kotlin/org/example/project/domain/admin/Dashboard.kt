package org.example.project.domain.admin

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.OrderId

@Immutable
data class RecentOrderSummary(
    val orderId: OrderId,
    val status: OrderStatus,
    val characterName: String,
    val totalPrice: Long,
    val totalCurrencyCode: String,
    val createdAt: Instant
)
