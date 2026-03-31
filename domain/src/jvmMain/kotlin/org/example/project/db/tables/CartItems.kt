package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CartItems : LongIdTable("cart_items") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)
    val quantity = integer("quantity").default(1)
    val addedAt = long("added_at").clientDefault { System.currentTimeMillis() }

    init {
        uniqueIndex(character, product)
    }
}
