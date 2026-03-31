package org.example.project.domain.model

data class CartItem(
    val id: Long,
    val characterId: Long,
    val productId: Long,
    val quantity: Int,
    val addedAt: Long
)
