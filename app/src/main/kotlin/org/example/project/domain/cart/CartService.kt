package org.example.project.domain.cart

import org.example.project.domain.cart.CartRepository
import org.example.project.domain.catalog.ProductRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.shared.CartItemId
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.cart.CartItem
import org.jetbrains.exposed.v1.jdbc.Database

class CartService(
    private val database: Database,
    private val cartRepository: CartRepository = CartRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) {
    suspend fun getCart(characterId: CharacterId): List<CartItem> =
        database.suspendTransaction { cartRepository.getCartItems(characterId) }

    suspend fun addToCart(
        characterId: CharacterId,
        productId: ProductId,
        quantity: Int = 1
    ): CartItemId {
        require(quantity > 0) { "Quantity must be positive" }
        return database.suspendTransaction {
            val product = productRepository.getProductOrNull(productId)
                ?: throw IllegalArgumentException("Product not found: $productId")
            require(product.isActive) { "Product is not active: $productId" }
            val existingQuantity = cartRepository.getCartItems(characterId)
                .firstOrNull { it.productId == productId }
                ?.quantity
                ?: 0
            val requestedQuantity = existingQuantity + quantity
            require(product.stock >= requestedQuantity) {
                "Insufficient stock for product: $productId"
            }
            cartRepository.addToCart(characterId, productId, quantity)
        }
    }

    suspend fun updateQuantity(cartItemId: CartItemId, quantity: Int): Boolean {
        require(quantity > 0) { "Quantity must be positive" }
        return database.suspendTransaction {
            val cartItem = cartRepository.getCartItemOrNull(cartItemId)
                ?: return@suspendTransaction false
            val product = productRepository.getProductOrNull(cartItem.productId)
                ?: throw IllegalStateException("Product not found: ${cartItem.productId}")
            require(product.isActive) { "Product is not active: ${cartItem.productId}" }
            require(product.stock >= quantity) {
                "Insufficient stock for product: ${cartItem.productId}"
            }
            cartRepository.updateQuantity(cartItemId, quantity)
        }
    }

    suspend fun removeFromCart(cartItemId: CartItemId): Boolean =
        database.suspendTransaction { cartRepository.removeFromCart(cartItemId) }

    suspend fun clearCart(characterId: CharacterId): Int =
        database.suspendTransaction { cartRepository.clearCart(characterId) }
}
