package org.example.project.db.repository

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.db.suspendTransaction
import org.example.project.domain.catalog.*
import org.example.project.domain.character.*
import org.example.project.domain.currency.*
import org.example.project.domain.order.*
import org.example.project.domain.review.*
import org.example.project.domain.shipping.*
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import kotlin.test.*

class ExposedReviewRepositoryTest {

    private lateinit var database: Database
    private lateinit var reviewRepo: ReviewRepository

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_review_repo_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        reviewRepo = ReviewRepository()
    }

    @Test
    fun testAverageRatingForProductOrNull() = runBlocking {
        database.suspendTransaction {
            // Seed necessary data
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }
            val mId = Merchants.insertAndGetId {
                it[name] = "Test Merchant"
            }
            val productId = ProductId(Products.insertAndGetId {
                it[name] = "Test Potion"
                it[category] = "POTIONS"
                it[rarity] = "COMMON"
                it[price] = 100
                it[currency] = goldId
                it[merchant] = mId
            }.value)

            val charId = Characters.insertAndGetId {
                it[name] = "Test Character"
            }
            
            val shipId = ShippingMethods.insertAndGetId {
                it[name] = "Test Shipping"
                it[baseCost] = 10
                it[currency] = goldId
                it[estimatedDays] = 1
            }

            val orderId = Orders.insertAndGetId {
                it[character] = charId
                it[status] = "DELIVERED"
                it[totalPrice] = 110
                it[totalCurrency] = goldId
            }

            val subOrderId = SubOrders.insertAndGetId {
                it[order] = orderId
                it[merchant] = mId
                it[status] = "DELIVERED"
                it[shippingMethod] = shipId
                it[shippingCost] = 10
                it[merchantTotalPrice] = 110
            }

            val orderItemId = OrderItemId(OrderItems.insertAndGetId {
                it[subOrder] = subOrderId
                it[product] = productId.value
                it[quantity] = 1
                it[snapshottedPrice] = 100
                it[snapshottedCurrency] = goldId
            }.value)

            // Test with no reviews
            assertNull(reviewRepo.averageRatingForProductOrNull(productId))

            // Add a review
            reviewRepo.createReview(
                characterId = CharacterId(charId.value),
                productId = productId,
                orderItemId = orderItemId,
                rating = 4,
                text = "Good"
            )

            assertEquals(4.0, reviewRepo.averageRatingForProductOrNull(productId))

            // Add another review
            val char2Id = Characters.insertAndGetId {
                it[name] = "Test Character 2"
            }
            val order2Id = Orders.insertAndGetId {
                it[character] = char2Id
                it[status] = "DELIVERED"
                it[totalPrice] = 110
                it[totalCurrency] = goldId
            }
            val subOrder2Id = SubOrders.insertAndGetId {
                it[order] = order2Id
                it[merchant] = mId
                it[status] = "DELIVERED"
                it[shippingMethod] = shipId
                it[shippingCost] = 10
                it[merchantTotalPrice] = 110
            }
            val orderItem2Id = OrderItemId(OrderItems.insertAndGetId {
                it[subOrder] = subOrder2Id
                it[product] = productId.value
                it[quantity] = 1
                it[snapshottedPrice] = 100
                it[snapshottedCurrency] = goldId
            }.value)

            reviewRepo.createReview(
                characterId = CharacterId(char2Id.value),
                productId = productId,
                orderItemId = orderItem2Id,
                rating = 5,
                text = "Great"
            )

            assertEquals(4.5, reviewRepo.averageRatingForProductOrNull(productId))
        }
    }
}
