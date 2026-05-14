package org.example.project.domain.wishlist

import org.example.project.domain.wishlist.WishlistItems
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.WishlistItemId
import org.example.project.domain.wishlist.WishlistItem
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.example.project.db.deleteById
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.springframework.stereotype.Service

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Service
class WishlistRepository {

    
    fun addToWishlist(characterId: CharacterId, productId: ProductId): WishlistItemId {
        val existing = WishlistItems.selectAll().where { 
            (WishlistItems.character eq characterId.value) and (WishlistItems.product eq productId.value) 
        }.singleOrNull()

        return if (existing != null) {
            WishlistItemId(existing[WishlistItems.id].value)
        } else {
            WishlistItemId(
                WishlistItems.insertAndGetId {
                    it[character] = characterId.value
                    it[product] = productId.value
                }.value
            )
        }
    }

    
    fun getWishlistItems(characterId: CharacterId): List<WishlistItem> =
        WishlistItems.selectAll().where { WishlistItems.character eq characterId.value }
            .map(::mapToWishlistItem)

    
    fun removeFromWishlist(wishlistItemId: WishlistItemId): Boolean =
        WishlistItems.deleteById(wishlistItemId.value)

    
    fun clearWishlist(characterId: CharacterId): Int =
        WishlistItems.deleteWhere { character eq characterId.value }

    private fun mapToWishlistItem(row: ResultRow) = WishlistItem(
        id = WishlistItemId(row[WishlistItems.id].value),
        characterId = CharacterId(row[WishlistItems.character].value),
        productId = ProductId(row[WishlistItems.product].value),
        createdAt = row[WishlistItems.createdAt],
        updatedAt = row[WishlistItems.updatedAt]
    )
}
