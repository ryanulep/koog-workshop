package org.example.project.domain.order

import androidx.compose.runtime.Immutable

@Immutable
data class OrderDetails(
    val order: Order,
    val subOrders: List<SubOrderDetails>
)

@Immutable
data class SubOrderDetails(
    val subOrder: SubOrder,
    val items: List<OrderItem>
)
