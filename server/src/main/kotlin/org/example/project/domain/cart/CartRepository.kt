package org.example.project.domain.cart

import org.example.project.db.deleteById
import org.example.project.db.findByIdOrNull
import org.example.project.db.update
import org.example.project.domain.cart.CartItems
import org.example.project.domain.shared.CartItemId
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.cart.CartItem
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.springframework.stereotype.Service

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Service
class CartRepository {

    fun addToCart(characterId: CharacterId, productId: ProductId, quantity: Int = 1): CartItemId {
        require(quantity > 0) { "Quantity must be positive" }
        val existing = CartItems.selectAll().where {
            (CartItems.character eq characterId.value) and (CartItems.product eq productId.value)
        }.singleOrNull()

        return if (existing != null) {
            val currentQuantity = existing[CartItems.quantity]
            val id = existing[CartItems.id].value
            CartItems.update(id) {
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

    fun getCartItems(characterId: CharacterId): List<CartItem> =
        CartItems.selectAll().where { CartItems.character eq characterId.value }
            .map(::mapToCartItem)

    fun getCartItemOrNull(cartItemId: CartItemId): CartItem? =
        CartItems.findByIdOrNull(cartItemId.value, ::mapToCartItem)

    fun updateQuantity(cartItemId: CartItemId, quantity: Int): Boolean {
        require(quantity > 0) { "Quantity must be positive" }
        return CartItems.update(cartItemId.value) {
            it[CartItems.quantity] = quantity
        } > 0
    }

    fun removeFromCart(cartItemId: CartItemId): Boolean =
        CartItems.deleteById(cartItemId.value)

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
