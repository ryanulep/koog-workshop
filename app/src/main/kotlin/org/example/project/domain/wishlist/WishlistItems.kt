package org.example.project.domain.wishlist

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters
import org.example.project.domain.catalog.Products

object WishlistItems : StoreTable("wishlist_items") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)

    init {
        uniqueIndex(character, product)
    }
}
