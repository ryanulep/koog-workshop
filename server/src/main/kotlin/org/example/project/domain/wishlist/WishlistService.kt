package org.example.project.domain.wishlist

import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.wishlist.WishlistRepository

import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.WishlistItemId
import org.example.project.domain.wishlist.WishlistItem
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class WishlistService(
    private val wishlistRepository: WishlistRepository,
    private val productRepository: ProductRepository
) {
    fun getWishlist(characterId: CharacterId): List<WishlistItem> =
        wishlistRepository.getWishlistItems(characterId)

    suspend fun addToWishlist(characterId: CharacterId, productId: ProductId): WishlistItemId {
        productRepository.getProductOrNull(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")
        return wishlistRepository.addToWishlist(characterId, productId)
    }

    fun removeFromWishlist(wishlistItemId: WishlistItemId): Boolean =
        wishlistRepository.removeFromWishlist(wishlistItemId)

    fun clearWishlist(characterId: CharacterId): Int =
        wishlistRepository.clearWishlist(characterId)
}
