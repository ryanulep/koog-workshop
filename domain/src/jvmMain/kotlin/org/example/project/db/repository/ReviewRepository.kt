package org.example.project.db.repository

import org.example.project.db.suspendTransaction
import org.example.project.db.tables.Reviews
import org.example.project.domain.model.Review
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class ReviewRepository(
    private val database: Database
) {

    suspend fun createReview(
        characterId: Long,
        productId: Long,
        orderItemId: Long,
        rating: Int,
        text: String?
    ): Long = database.suspendTransaction {
        Reviews.insertAndGetId {
            it[Reviews.character] = characterId
            it[Reviews.product] = productId
            it[Reviews.orderItem] = orderItemId
            it[Reviews.rating] = rating
            it[Reviews.text] = text
        }.value
    }

    suspend fun getReviewsForProduct(productId: Long): List<Review> = database.suspendTransaction {
        Reviews.selectAll().where { Reviews.product eq productId }
            .map(::mapToReview)
    }

    suspend fun getAverageRatingForProduct(productId: Long): Double = database.suspendTransaction {
        val ratings = Reviews.selectAll().where { Reviews.product eq productId }
            .map { it[Reviews.rating] }
        if (ratings.isEmpty()) 0.0 else ratings.average()
    }

    private fun mapToReview(row: ResultRow) = Review(
        id = row[Reviews.id].value,
        characterId = row[Reviews.character].value,
        productId = row[Reviews.product].value,
        orderItemId = row[Reviews.orderItem].value,
        rating = row[Reviews.rating],
        text = row[Reviews.text],
        createdAt = row[Reviews.createdAt],
        updatedAt = row[Reviews.updatedAt]
    )
}
