package org.example.project.domain.model

data class Review(
    val id: Long,
    val characterId: Long,
    val productId: Long,
    val orderItemId: Long,
    val rating: Int,
    val text: String?,
    val createdAt: Long,
    val updatedAt: Long
)
