package org.example.project.domain.wishlist

import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.wishlist.WishlistRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.WishlistItemId
import org.example.project.domain.wishlist.WishlistItem
import org.jetbrains.exposed.v1.jdbc.Database

class WishlistService(
    private val database: Database,
    private val wishlistRepository: WishlistRepository = WishlistRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) {
    suspend fun getWishlist(characterId: CharacterId): List<WishlistItem> =
        database.suspendTransaction { wishlistRepository.getWishlistItems(characterId) }

    suspend fun addToWishlist(characterId: CharacterId, productId: ProductId): WishlistItemId =
        database.suspendTransaction {
            productRepository.getProductOrNull(productId)
                ?: throw IllegalArgumentException("Product not found: $productId")
            wishlistRepository.addToWishlist(characterId, productId)
        }

    suspend fun removeFromWishlist(wishlistItemId: WishlistItemId): Boolean =
        database.suspendTransaction { wishlistRepository.removeFromWishlist(wishlistItemId) }

    suspend fun clearWishlist(characterId: CharacterId): Int =
        database.suspendTransaction { wishlistRepository.clearWishlist(characterId) }
}
