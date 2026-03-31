package org.example.project.db.repository

import org.example.project.db.suspendTransaction
import org.example.project.db.tables.Reviews
import org.example.project.domain.id.*
import org.example.project.domain.model.Review
import org.example.project.domain.repository.ReviewRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class ExposedReviewRepository(
    private val database: Database
) : ReviewRepository {

    override suspend fun createReview(
        characterId: CharacterId,
        productId: ProductId,
        orderItemId: OrderItemId,
        rating: Int,
        text: String?
    ): ReviewId = database.suspendTransaction {
        ReviewId(
            Reviews.insertAndGetId {
                it[character] = characterId.value
                it[product] = productId.value
                it[orderItem] = orderItemId.value
                it[Reviews.rating] = rating
                it[Reviews.text] = text
            }.value
        )
    }

    override suspend fun getReviewsForProduct(productId: ProductId): List<Review> = database.suspendTransaction {
        Reviews.selectAll().where { Reviews.product eq productId.value }
            .map(::mapToReview)
    }

    override suspend fun getAverageRatingForProduct(productId: ProductId): Double = database.suspendTransaction {
        Reviews.selectAll().where { Reviews.product eq productId.value }
            .map { it[Reviews.rating] }
            .average()
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
