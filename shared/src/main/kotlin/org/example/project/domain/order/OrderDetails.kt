package org.example.project.domain.order

import kotlinx.serialization.Serializable

@Serializable
data class OrderDetails(
    val order: Order,
    val subOrders: List<SubOrderDetails>
)

@Serializable
data class SubOrderDetails(
    val subOrder: SubOrder,
    val items: List<OrderItem>
)

@Serializable
data class CheckoutRequest(
    val shippingSelections: Map<String, String>
)
