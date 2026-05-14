package org.example.project.domain.review

import org.example.project.domain.order.OrderRepository
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.review.ReviewRepository

import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.OrderItemId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.ReviewId
import org.example.project.domain.shared.Page
import org.example.project.domain.review.Review
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val orderRepository: OrderRepository
) {
    fun createReview(
        characterId: CharacterId,
        productId: ProductId,
        orderItemId: OrderItemId,
        rating: Int,
        text: String? = null
    ): ReviewId {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
        val orderItem = orderRepository.getOrderItemOrNull(orderItemId)
            ?: throw IllegalArgumentException("Order item not found: $orderItemId")
        require(orderItem.productId == productId) {
            "Order item $orderItemId does not match product $productId"
        }

        val subOrder = orderRepository.getSubOrderOrNull(orderItem.subOrderId)
            ?: throw IllegalArgumentException("Sub-order not found for order item: $orderItemId")
        val order = orderRepository.getOrderOrNull(subOrder.orderId)
            ?: throw IllegalArgumentException("Order not found for order item: $orderItemId")

        require(order.characterId == characterId) {
            "Order item $orderItemId does not belong to character $characterId"
        }
        require(subOrder.status == OrderStatus.DELIVERED) {
            "Order item $orderItemId is not eligible for review: sub-order status ${subOrder.status}"
        }
        require(
            reviewRepository.getReviewForCharacterAndOrderItemOrNull(characterId, orderItemId) == null
        ) {
            "Review already exists for order item: $orderItemId"
        }

        return reviewRepository.createReview(characterId, productId, orderItemId, rating, text)
    }

    fun updateReview(id: ReviewId, rating: Int? = null, text: String? = null): Boolean {
        if (rating != null) require(rating in 1..5) { "Rating must be between 1 and 5" }
        return reviewRepository.updateReview(id, rating, text)
    }

    fun deleteReview(id: ReviewId): Boolean =
        reviewRepository.deleteReview(id)

    fun getReviewOrNull(id: ReviewId): Review? =
        reviewRepository.getReviewOrNull(id)

    fun getProductReviews(
        productId: ProductId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Review> =
        reviewRepository.getReviewsForProduct(productId, offset, limit)

    fun getAverageRatingOrNull(productId: ProductId): Double? =
        reviewRepository.averageRatingForProductOrNull(productId)
}
