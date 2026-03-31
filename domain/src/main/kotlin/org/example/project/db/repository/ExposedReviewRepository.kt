package org.example.project.db.repository

import org.example.project.db.tables.Reviews
import org.example.project.domain.id.*
import org.example.project.domain.model.Review
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class ExposedReviewRepository {

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
    fun getReviewsForProduct(productId: ProductId): List<Review> =
        Reviews.selectAll().where { Reviews.product eq productId.value }
            .map(::mapToReview)

    context(_: Transaction)
    fun getAverageRatingForProduct(productId: ProductId): Double =
        Reviews.selectAll().where { Reviews.product eq productId.value }
            .map { it[Reviews.rating] }
            .average()

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
