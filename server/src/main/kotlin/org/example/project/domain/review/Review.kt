package org.example.project.domain.review


import kotlin.time.Instant
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.OrderItemId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.ReviewId


data class Review(
    val id: ReviewId,
    val characterId: CharacterId,
    val productId: ProductId,
    val orderItemId: OrderItemId,
    val rating: Int,
    val text: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
