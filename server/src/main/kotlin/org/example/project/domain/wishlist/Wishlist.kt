package org.example.project.domain.wishlist


import kotlin.time.Instant
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.WishlistItemId


data class WishlistItem(
    val id: WishlistItemId,
    val characterId: CharacterId,
    val productId: ProductId,
    val createdAt: Instant,
    val updatedAt: Instant
)
