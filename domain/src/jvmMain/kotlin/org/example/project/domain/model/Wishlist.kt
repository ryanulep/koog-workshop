package org.example.project.domain.model

data class WishlistItem(
    val id: Long,
    val characterId: Long,
    val productId: Long,
    val addedAt: Long
)
