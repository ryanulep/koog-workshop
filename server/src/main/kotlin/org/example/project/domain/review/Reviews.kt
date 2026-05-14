package org.example.project.domain.review

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters
import org.example.project.domain.catalog.Products
import org.example.project.domain.order.OrderItems
import org.jetbrains.exposed.v1.core.between

object Reviews : StoreTable("reviews") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)
    val orderItem = reference("order_item_id", OrderItems)
    val rating = integer("rating").check { it.between(1, 5) }
    val text = text("text").nullable()

    init {
        uniqueIndex(character, orderItem)
        index(false, product)
        index(false, character)
    }
}
