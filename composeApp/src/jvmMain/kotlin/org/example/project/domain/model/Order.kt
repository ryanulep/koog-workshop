package org.example.project.domain.model

import org.example.project.domain.enums.OrderStatus

data class Order(
    val id: Long,
    val characterId: Long,
    val status: OrderStatus,
    val totalPrice: Long,
    val totalCurrencyId: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class SubOrder(
    val id: Long,
    val orderId: Long,
    val merchantId: Long,
    val status: OrderStatus,
    val shippingMethodId: Long,
    val shippingCost: Long,
    val merchantTotalPrice: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class OrderItem(
    val id: Long,
    val subOrderId: Long,
    val productId: Long,
    val quantity: Int,
    val snapshottedPrice: Long,
    val snapshottedCurrencyId: Long,
    val createdAt: Long
)
