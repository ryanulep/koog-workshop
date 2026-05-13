package org.example.project.domain.review

import org.example.project.db.deleteById
import org.example.project.db.findByIdOrNull
import org.example.project.db.update
import org.example.project.domain.review.Reviews
import org.example.project.domain.shared.*
import org.example.project.domain.review.Review
import org.example.project.domain.shared.Page
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Service

@Service
class ReviewRepository {

    
    fun createReview(
        characterId: CharacterId,
        productId: ProductId,
        orderItemId: OrderItemId,
        rating: Int,
        text: String?
    ): ReviewId =
        ReviewId(
            Reviews.insertAndGetId {
                it[character] = characterId.value
                it[product] = productId.value
                it[orderItem] = orderItemId.value
                it[Reviews.rating] = rating
                it[Reviews.text] = text
            }.value
        )

    
    fun getReviewsForProduct(productId: ProductId, offset: Long, limit: Long): Page<Review> {
        val query = Reviews.selectAll().where { Reviews.product eq productId.value }
        val items = query.copy()
            .limit(limit.toInt()).offset(offset)
            .map(::mapToReview)
        val total = query.count()
        return Page(items, total, offset, limit)
    }

    
    fun getReviewsForProduct(productId: ProductId): List<Review> =
        Reviews.selectAll().where { Reviews.product eq productId.value }
            .map(::mapToReview)

    
    fun getReviewOrNull(id: ReviewId): Review? =
        Reviews.findByIdOrNull(id.value, ::mapToReview)

    
    fun getReviewForCharacterAndOrderItemOrNull(
        characterId: CharacterId,
        orderItemId: OrderItemId
    ): Review? =
        Reviews.selectAll().where {
            (Reviews.character eq characterId.value) and (Reviews.orderItem eq orderItemId.value)
        }
            .map(::mapToReview)
            .singleOrNull()

    
    fun updateReview(id: ReviewId, rating: Int? = null, text: String? = null): Boolean =
        Reviews.update(id.value) {
            if (rating != null) it[Reviews.rating] = rating
            if (text != null) it[Reviews.text] = text
        } > 0

    
    fun deleteReview(id: ReviewId): Boolean =
        Reviews.deleteById(id.value)

    
    fun averageRatingForProductOrNull(productId: ProductId): Double? {
        val reviews = Reviews.selectAll().where { Reviews.product eq productId.value }
            .map { it[Reviews.rating] }

        return if (reviews.isEmpty()) null else reviews.average()
    }

    private fun mapToReview(row: ResultRow) = Review(
        id = ReviewId(row[Reviews.id].value),
        characterId = CharacterId(row[Reviews.character].value),
        productId = ProductId(row[Reviews.product].value),
        orderItemId = OrderItemId(row[Reviews.orderItem].value),
        rating = row[Reviews.rating],
        text = row[Reviews.text],
        createdAt = row[Reviews.createdAt],
        updatedAt = row[Reviews.updatedAt]
    )
}
