package org.example.project.domain.cart

import org.example.project.domain.cart.CartRepository
import org.example.project.domain.catalog.ProductRepository

import org.example.project.domain.shared.CartItemId
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.cart.CartItem
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class CartService(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository
) {
    fun getCart(characterId: CharacterId): List<CartItem> =
        cartRepository.getCartItems(characterId)

    fun addToCart(
        characterId: CharacterId,
        productId: ProductId,
        quantity: Int = 1
    ): CartItemId {
        require(quantity > 0) { "Quantity must be positive" }

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
        return cartRepository.addToCart(characterId, productId, quantity)
    }

    fun updateQuantity(cartItemId: CartItemId, quantity: Int): Boolean {
        require(quantity > 0) { "Quantity must be positive" }
        val cartItem = cartRepository.getCartItemOrNull(cartItemId) ?: return false
        val product = productRepository.getProductOrNull(cartItem.productId)
            ?: throw IllegalStateException("Product not found: ${cartItem.productId}")
        require(product.isActive) { "Product is not active: ${cartItem.productId}" }
        require(product.stock >= quantity) {
            "Insufficient stock for product: ${cartItem.productId}"
        }
        return cartRepository.updateQuantity(cartItemId, quantity)
    }

    fun removeFromCart(cartItemId: CartItemId): Boolean =
        cartRepository.removeFromCart(cartItemId)

    fun clearCart(characterId: CharacterId): Int =
        cartRepository.clearCart(characterId)
}
