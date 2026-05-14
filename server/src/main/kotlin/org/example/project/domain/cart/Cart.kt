package org.example.project.domain.cart

import kotlin.time.Instant
import org.example.project.domain.shared.CartItemId
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId

data class CartItem(
    val id: CartItemId,
    val characterId: CharacterId,
    val productId: ProductId,
    val quantity: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
