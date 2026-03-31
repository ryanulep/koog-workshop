package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object WishlistItems : LongIdTable("wishlist_items") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)
    val addedAt = long("added_at").clientDefault { System.currentTimeMillis() }

    init {
        uniqueIndex(character, product)
    }
}
