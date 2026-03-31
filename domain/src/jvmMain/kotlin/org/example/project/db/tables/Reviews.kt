package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.between

object Reviews : LongIdTable("reviews") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)
    val orderItem = reference("order_item_id", OrderItems)
    val rating = integer("rating").check { it.between(1, 5) }
    val text = text("text").nullable()
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val updatedAt = long("updated_at").clientDefault { System.currentTimeMillis() }

    init {
        uniqueIndex(character, orderItem)
        index(false, product)
        index(false, character)
    }
}
