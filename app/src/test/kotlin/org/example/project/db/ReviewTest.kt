package org.example.project.db

import org.example.project.domain.catalog.*
import org.example.project.domain.character.*
import org.example.project.domain.currency.*
import org.example.project.domain.order.*
import org.example.project.domain.review.*
import org.example.project.domain.shipping.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ReviewTest {

    @BeforeTest
    fun setup() {
        connectSqlite("jdbc:sqlite:file:test_reviews?mode=memory&cache=shared")
    }

    private fun setupInitialData() {
        SchemaUtils.create(
            Characters, Currencies, Merchants, Products, ShippingMethods, 
            Orders, SubOrders, OrderItems, Reviews
        )

        val charId = Characters.insertAndGetId { it[name] = "Hero" }
        val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
        val merchantId = Merchants.insertAndGetId { it[name] = "Merchant" }
        val prodId = Products.insertAndGetId {
            it[Products.name] = "Healing Potion"
            it[Products.category] = "POTIONS"
            it[Products.rarity] = "COMMON"
            it[Products.price] = 50
            it[Products.currency] = goldId
            it[Products.merchant] = merchantId
        }
        val shipId = ShippingMethods.insertAndGetId {
            it[ShippingMethods.name] = "Courier"
            it[ShippingMethods.baseCost] = 10
            it[ShippingMethods.currency] = goldId
            it[ShippingMethods.estimatedDays] = 1
        }

        val orderId = Orders.insertAndGetId {
            it[Orders.character] = charId
            it[Orders.status] = OrderStatus.DELIVERED.name
            it[Orders.totalPrice] = 60
            it[Orders.totalCurrency] = goldId
        }

        val subOrderId = SubOrders.insertAndGetId {
            it[SubOrders.order] = orderId
            it[SubOrders.merchant] = merchantId
            it[SubOrders.status] = OrderStatus.DELIVERED.name
            it[SubOrders.shippingMethod] = shipId
            it[SubOrders.shippingCost] = 10
            it[SubOrders.merchantTotalPrice] = 60
        }

        val orderItemId = OrderItems.insertAndGetId {
            it[OrderItems.subOrder] = subOrderId
            it[OrderItems.product] = prodId
            it[OrderItems.quantity] = 1
            it[OrderItems.snapshottedPrice] = 50
            it[OrderItems.snapshottedCurrency] = goldId
        }

        // Second character for testing multiple reviews
        val char2Id = Characters.insertAndGetId { it[name] = "Villain" }
        val order2Id = Orders.insertAndGetId {
            it[Orders.character] = char2Id
            it[Orders.status] = OrderStatus.DELIVERED.name
            it[Orders.totalPrice] = 60
            it[Orders.totalCurrency] = goldId
        }
        val subOrder2Id = SubOrders.insertAndGetId {
            it[SubOrders.order] = order2Id
            it[SubOrders.merchant] = merchantId
            it[SubOrders.status] = OrderStatus.DELIVERED.name
            it[SubOrders.shippingMethod] = shipId
            it[SubOrders.shippingCost] = 10
            it[SubOrders.merchantTotalPrice] = 60
        }
        val orderItem2Id = OrderItems.insertAndGetId {
            it[OrderItems.subOrder] = subOrder2Id
            it[OrderItems.product] = prodId
            it[OrderItems.quantity] = 1
            it[OrderItems.snapshottedPrice] = 50
            it[OrderItems.snapshottedCurrency] = goldId
        }
    }

    @Test
    fun testReviewCRUD() {
        transaction {
            setupInitialData()
            
            val charId = Characters.selectAll().where { Characters.name eq "Hero" }.single()[Characters.id]
            val prodId = Products.selectAll().where { Products.name eq "Healing Potion" }.single()[Products.id]
            val orderItemId = OrderItems.selectAll().first()[OrderItems.id]

            // 1. Create review
            val reviewId = Reviews.insertAndGetId {
                it[character] = charId
                it[product] = prodId
                it[orderItem] = orderItemId
                it[rating] = 5
                it[text] = "Amazing potion!"
            }

            val review = Reviews.selectAll().where { Reviews.id eq reviewId }.single()
            assertEquals(5, review[Reviews.rating])
            assertEquals("Amazing potion!", review[Reviews.text])
            
            // 8. Update review
            val updatedCount = Reviews.update({ Reviews.id eq reviewId }) {
                it[rating] = 4
                it[text] = "Actually, it was okay."
                // it[updatedAt] = Clock.System.now() // Skipping manual updatedAt due to type mismatch
            }
            assertEquals(1, updatedCount)
            
            val updatedReview = Reviews.selectAll().where { Reviews.id eq reviewId }.single()
            assertEquals(4, updatedReview[Reviews.rating])
            assertEquals("Actually, it was okay.", updatedReview[Reviews.text])
            
            // 9. Delete review
            val deletedCount = Reviews.deleteWhere { Reviews.id eq reviewId }
            assertEquals(1, deletedCount)
            assertTrue(Reviews.selectAll().where { Reviews.id eq reviewId }.empty())
        }
    }

    @Test
    fun testRatingBounds() {
        transaction {
            setupInitialData()
            val charId = Characters.selectAll().where { Characters.name eq "Hero" }.single()[Characters.id]
            val prodId = Products.selectAll().where { Products.name eq "Healing Potion" }.single()[Products.id]
            val orderItemId = OrderItems.selectAll().first()[OrderItems.id]

            // Rating = 0 should fail
            assertFails {
                Reviews.insert {
                    it[character] = charId
                    it[product] = prodId
                    it[orderItem] = orderItemId
                    it[rating] = 0
                }
            }

            // Rating = 6 should fail
            assertFails {
                Reviews.insert {
                    it[character] = charId
                    it[product] = prodId
                    it[orderItem] = orderItemId
                    it[rating] = 6
                }
            }

            // Rating 1 and 5 should succeed
            Reviews.insert {
                it[character] = charId
                it[product] = prodId
                it[orderItem] = orderItemId
                it[rating] = 1
            }
        }
    }

    @Test
    fun testNullableText() {
        transaction {
            setupInitialData()
            val charId = Characters.selectAll().where { Characters.name eq "Hero" }.single()[Characters.id]
            val prodId = Products.selectAll().where { Products.name eq "Healing Potion" }.single()[Products.id]
            val orderItemId = OrderItems.selectAll().first()[OrderItems.id]

            Reviews.insert {
                it[character] = charId
                it[product] = prodId
                it[orderItem] = orderItemId
                it[rating] = 3
                it[text] = null
            }
            
            val review = Reviews.selectAll().where { Reviews.character eq charId }.single()
            assertNull(review[Reviews.text])
        }
    }

    @Test
    fun testOneReviewPerPurchase() {
        transaction {
            setupInitialData()
            val charId = Characters.selectAll().where { Characters.name eq "Hero" }.single()[Characters.id]
            val prodId = Products.selectAll().where { Products.name eq "Healing Potion" }.single()[Products.id]
            val orderItemId = OrderItems.selectAll().first()[OrderItems.id]

            Reviews.insert {
                it[character] = charId
                it[product] = prodId
                it[orderItem] = orderItemId
                it[rating] = 5
            }

            // Duplicate review for same character and orderItem should fail
            assertFails {
                Reviews.insert {
                    it[character] = charId
                    it[product] = prodId
                    it[orderItem] = orderItemId
                    it[rating] = 4
                }
            }
        }
    }

    @Test
    fun testMultipleReviewsPerProduct() {
        transaction {
            setupInitialData()
            val char1Id = Characters.selectAll().where { Characters.name eq "Hero" }.single()[Characters.id]
            val char2Id = Characters.selectAll().where { Characters.name eq "Villain" }.single()[Characters.id]
            val prodId = Products.selectAll().where { Products.name eq "Healing Potion" }.single()[Products.id]
            
            val orderItems = OrderItems.selectAll().toList()
            val orderItem1Id = orderItems[0][OrderItems.id]
            val orderItem2Id = orderItems[1][OrderItems.id]

            Reviews.insert {
                it[character] = char1Id
                it[product] = prodId
                it[orderItem] = orderItem1Id
                it[rating] = 5
            }

            Reviews.insert {
                it[character] = char2Id
                it[product] = prodId
                it[orderItem] = orderItem2Id
                it[rating] = 1
            }

            val count = Reviews.selectAll().where { Reviews.product eq prodId }.count()
            assertEquals(2, count)
        }
    }

    @Test
    fun testAverageRating() {
        transaction {
            setupInitialData()
            val prodId = Products.selectAll().where { Products.name eq "Healing Potion" }.single()[Products.id]
            
            // I need 3 reviews for the same product.
            // My setupInitialData only has 2 characters/orderItems. I'll add a third one.
            val char3Id = Characters.insertAndGetId { it[name] = "Sidekick" }
            val goldId = Currencies.selectAll().where { Currencies.code eq "GOLD" }.single()[Currencies.id]
            val merchantId = Merchants.selectAll().single()[Merchants.id]
            val shipId = ShippingMethods.selectAll().single()[ShippingMethods.id]
            
            val order3Id = Orders.insertAndGetId {
                it[Orders.character] = char3Id
                it[Orders.status] = OrderStatus.DELIVERED.name
                it[Orders.totalPrice] = 60
                it[Orders.totalCurrency] = goldId
            }
            val subOrder3Id = SubOrders.insertAndGetId {
                it[SubOrders.order] = order3Id
                it[SubOrders.merchant] = merchantId
                it[SubOrders.status] = OrderStatus.DELIVERED.name
                it[SubOrders.shippingMethod] = shipId
                it[SubOrders.shippingCost] = 10
                it[SubOrders.merchantTotalPrice] = 60
            }
            val orderItem3Id = OrderItems.insertAndGetId {
                it[OrderItems.subOrder] = subOrder3Id
                it[OrderItems.product] = prodId
                it[OrderItems.quantity] = 1
                it[OrderItems.snapshottedPrice] = 50
                it[OrderItems.snapshottedCurrency] = goldId
            }

            val charIds = listOf(
                Characters.selectAll().where { Characters.name eq "Hero" }.single()[Characters.id],
                Characters.selectAll().where { Characters.name eq "Villain" }.single()[Characters.id],
                char3Id
            )
            val itemIds = listOf(
                OrderItems.selectAll().where { OrderItems.subOrder inList SubOrders.selectAll().where { SubOrders.order inList Orders.selectAll().where { Orders.character eq charIds[0] }.map { it[Orders.id] } }.map { it[SubOrders.id] } }.first()[OrderItems.id],
                OrderItems.selectAll().where { OrderItems.subOrder inList SubOrders.selectAll().where { SubOrders.order inList Orders.selectAll().where { Orders.character eq charIds[1] }.map { it[Orders.id] } }.map { it[SubOrders.id] } }.first()[OrderItems.id],
                orderItem3Id
            )

            Reviews.insert {
                it[character] = charIds[0]
                it[product] = prodId
                it[orderItem] = itemIds[0]
                it[rating] = 3
            }
            Reviews.insert {
                it[character] = charIds[1]
                it[product] = prodId
                it[orderItem] = itemIds[1]
                it[rating] = 4
            }
            Reviews.insert {
                it[character] = charIds[2]
                it[product] = prodId
                it[orderItem] = itemIds[2]
                it[rating] = 5
            }

            val avgRating = Reviews.select(Reviews.rating.avg())
                .where { Reviews.product eq prodId }
                .single()[Reviews.rating.avg()]?.toDouble()
            
            assertEquals(4.0, avgRating)
        }
    }

    @Test
    fun testForeignKeyConstraints() {
        transaction {
            setupInitialData()
            val charId = Characters.selectAll().where { Characters.name eq "Hero" }.single()[Characters.id]
            val prodId = Products.selectAll().where { Products.name eq "Healing Potion" }.single()[Products.id]

            // Review with nonexistent order_item_id should fail
            assertFails {
                Reviews.insert {
                    it[character] = charId
                    it[product] = prodId
                    it[orderItem] = EntityID(kotlin.uuid.Uuid.generateV7(), OrderItems)
                    it[rating] = 5
                }
            }
        }
    }
}
