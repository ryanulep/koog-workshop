package org.example.project.db.repository

import org.example.project.db.tables.CartItems
import org.example.project.domain.id.CartItemId
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.ProductId
import org.example.project.domain.model.CartItem
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class CartRepository {

    context(_: Transaction)
    fun addToCart(characterId: CharacterId, productId: ProductId, quantity: Int = 1): CartItemId {
        val existing = CartItems.selectAll().where { 
            (CartItems.character eq characterId.value) and (CartItems.product eq productId.value) 
        }.singleOrNull()

        return if (existing != null) {
            val currentQuantity = existing[CartItems.quantity]
            val id = existing[CartItems.id].value
            CartItems.update({ CartItems.id eq id }) {
                it[CartItems.quantity] = currentQuantity + quantity
            }
            CartItemId(id)
        } else {
            CartItemId(
                CartItems.insertAndGetId {
                    it[character] = characterId.value
                    it[product] = productId.value
                    it[CartItems.quantity] = quantity
                }.value
            )
        }
    }

    context(_: Transaction)
    fun getCartItems(characterId: CharacterId): List<CartItem> =
        CartItems.selectAll().where { CartItems.character eq characterId.value }
            .map(::mapToCartItem)

    context(_: Transaction)
    fun updateQuantity(cartItemId: CartItemId, quantity: Int): Boolean =
        CartItems.update({ CartItems.id eq cartItemId.value }) {
            it[CartItems.quantity] = quantity
        } > 0

    context(_: Transaction)
    fun removeFromCart(cartItemId: CartItemId): Boolean =
        CartItems.deleteWhere { id eq cartItemId.value } > 0

    context(_: Transaction)
    fun clearCart(characterId: CharacterId): Int =
        CartItems.deleteWhere { character eq characterId.value }

    private fun mapToCartItem(row: ResultRow) = CartItem(
        id = CartItemId(row[CartItems.id].value),
        characterId = CharacterId(row[CartItems.character].value),
        productId = ProductId(row[CartItems.product].value),
        quantity = row[CartItems.quantity],
        createdAt = row[CartItems.createdAt],
        updatedAt = row[CartItems.updatedAt]
    )
}
