package org.example.project.domain.order

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    CRAFTING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
