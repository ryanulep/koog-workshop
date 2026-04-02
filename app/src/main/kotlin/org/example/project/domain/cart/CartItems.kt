package org.example.project.domain.cart

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters
import org.example.project.domain.catalog.Products
import org.jetbrains.exposed.v1.core.greaterEq

object CartItems : StoreTable("cart_items") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)
    val quantity = integer("quantity").check("quantity_positive") { it greaterEq 1 }.default(1)

    init {
        uniqueIndex(character, product)
    }
}
