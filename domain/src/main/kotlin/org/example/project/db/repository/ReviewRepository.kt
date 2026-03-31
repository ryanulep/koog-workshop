package org.example.project.db.repository

import org.example.project.db.tables.Reviews
import org.example.project.domain.id.*
import org.example.project.domain.model.Review
import org.example.project.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class ReviewRepository {

    context(_: Transaction)
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

    context(_: Transaction)
    fun getReviewsForProduct(productId: ProductId, offset: Long, limit: Long): Page<Review> {
        val query = Reviews.selectAll().where { Reviews.product eq productId.value }
        val items = query.copy()
            .limit(limit.toInt()).offset(offset)
            .map(::mapToReview)
        val total = query.count()
        return Page(items, total, offset, limit)
    }

    context(_: Transaction)
    fun getReviewsForProduct(productId: ProductId, chunkSize: Long = 50L): Flow<List<Review>> = flow {
        var offset = 0L
        while (true) {
            val page =  getReviewsForProduct(productId, offset, chunkSize)
            if (page.items.isEmpty()) break
            emit(page.items)
            offset += chunkSize
        }
    }

    context(_: Transaction)
    fun getReviewsForProduct(productId: ProductId): List<Review> =
        Reviews.selectAll().where { Reviews.product eq productId.value }
            .map(::mapToReview)

    context(_: Transaction)
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
