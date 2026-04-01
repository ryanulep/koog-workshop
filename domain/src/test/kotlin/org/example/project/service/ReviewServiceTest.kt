package org.example.project.domain.review

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.db.suspendTransaction
import org.example.project.domain.cart.CartService
import org.example.project.domain.catalog.*
import org.example.project.domain.character.CharacterService
import org.example.project.domain.currency.CurrencyService
import org.example.project.domain.order.OrderRepository
import org.example.project.domain.order.OrderService
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shipping.ShippingService
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ReviewServiceTest {
    private lateinit var database: Database
    private lateinit var reviewService: ReviewService
    private lateinit var orderService: OrderService
    private lateinit var orderRepository: OrderRepository
    private lateinit var cartService: CartService
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private lateinit var characterService: CharacterService
    private lateinit var shippingService: ShippingService

    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.generateV7())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.generateV7())
    private var characterId: CharacterId = CharacterId(kotlin.uuid.Uuid.generateV7())
    private var productId: ProductId = ProductId(kotlin.uuid.Uuid.generateV7())
    private var orderItemId: OrderItemId = OrderItemId(kotlin.uuid.Uuid.generateV7())
    private var shippingMethodId: ShippingMethodId = ShippingMethodId(kotlin.uuid.Uuid.generateV7())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_review_", ".db").apply { deleteOnExit() }
        database = Database.connect("jdbc:sqlite:${testDbFile.absolutePath}").createTables()

        reviewService = ReviewService(database)
        orderService = OrderService(database)
        orderRepository = OrderRepository()
        cartService = CartService(database)
        catalogService = CatalogService(database)
        currencyService = CurrencyService(database)
        characterService = CharacterService(database)
        shippingService = ShippingService(database)

        runBlocking {
            goldId = currencyService.createCurrency("GOLD", "Gold", "G")
            merchantId = catalogService.createMerchant("Test Merchant")
            characterId = characterService.createCharacter("Test Hero")
            shippingMethodId = shippingService.createShippingMethod(
                name = "Courier Raven", baseCost = 50, currencyId = goldId, estimatedDays = 3
            )
            shippingService.addShippingMethodToMerchant(merchantId, shippingMethodId)

            productId = catalogService.createProduct(
                Product.Weapon(
                    id = ProductId(kotlin.uuid.Uuid.generateV7()),
                    name = "Test Sword", description = null, rarity = Rarity.COMMON,
                    price = 100, currencyId = goldId, merchantId = merchantId, stock = 10,
                    imageUrl = null, isActive = true,
                    createdAt = kotlin.time.Instant.DISTANT_PAST, updatedAt = kotlin.time.Instant.DISTANT_PAST,
                    damage = 5, damageType = DamageType.PHYSICAL, weaponSlot = WeaponSlot.MAIN_HAND
                )
            )
            characterService.deposit(characterId, goldId, 5000)

            // Create an order so we have a valid orderItemId
            cartService.addToCart(characterId, productId, 1)
            val orderId = orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))
            val details = orderService.getOrderDetailsOrNull(orderId)!!
            orderItemId = details.subOrders.first().items.first().id
            database.suspendTransaction {
                orderRepository.updateSubOrderStatus(details.subOrders.first().subOrder.id, OrderStatus.DELIVERED)
            }
        }
    }

    @Test
    fun testCreateAndGetReview() = runBlocking {
        val reviewId = reviewService.createReview(characterId, productId, orderItemId, 5, "Great sword!")

        val review = reviewService.getReview(reviewId)
        assertNotNull(review)
        assertEquals(characterId, review.characterId)
        assertEquals(productId, review.productId)
        assertEquals(orderItemId, review.orderItemId)
        assertEquals(5, review.rating)
        assertEquals("Great sword!", review.text)
    }

    @Test
    fun testUpdateReview() = runBlocking {
        val reviewId = reviewService.createReview(characterId, productId, orderItemId, 5, "Great sword!")

        val updated = reviewService.updateReview(reviewId, rating = 3)
        assertTrue(updated)

        val review = reviewService.getReview(reviewId)
        assertNotNull(review)
        assertEquals(3, review.rating)
    }

    @Test
    fun testDeleteReview() = runBlocking {
        val reviewId = reviewService.createReview(characterId, productId, orderItemId, 5, "Great sword!")

        val deleted = reviewService.deleteReview(reviewId)
        assertTrue(deleted)

        val review = reviewService.getReview(reviewId)
        assertNull(review)
    }

    @Test
    fun testGetProductReviews() = runBlocking {
        reviewService.createReview(characterId, productId, orderItemId, 5, "Great sword!")

        val reviews = reviewService.getProductReviews(productId)
        assertEquals(1, reviews.items.size)
    }

    @Test
    fun testGetAverageRating() = runBlocking {
        reviewService.createReview(characterId, productId, orderItemId, 4, "Good sword!")

        val avgRating = reviewService.getAverageRating(productId)
        assertNotNull(avgRating)
        assertEquals(4.0, avgRating)
    }

    @Test
    fun testInvalidRatingFails() = runBlocking {
        val exception = assertFailsWith<IllegalArgumentException> {
            reviewService.createReview(characterId, productId, orderItemId, 0, "Bad rating")
        }
        assertTrue(exception.message!!.contains("Rating must be between 1 and 5"))
    }

    @Test
    fun testReviewRejectsMismatchedProduct() = runBlocking {
        val otherProductId = catalogService.createProduct(
            Product.Weapon(
                id = ProductId(kotlin.uuid.Uuid.generateV7()),
                name = "Wrong Sword", description = null, rarity = Rarity.COMMON,
                price = 120, currencyId = goldId, merchantId = merchantId, stock = 5,
                imageUrl = null, isActive = true,
                createdAt = kotlin.time.Instant.DISTANT_PAST, updatedAt = kotlin.time.Instant.DISTANT_PAST,
                damage = 3, damageType = DamageType.PHYSICAL, weaponSlot = WeaponSlot.MAIN_HAND
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            reviewService.createReview(characterId, otherProductId, orderItemId, 5, "Wrong product")
        }
        assertTrue(exception.message!!.contains("does not match product"))
    }

    @Test
    fun testReviewRejectsWrongCharacter() = runBlocking {
        val otherCharacterId = characterService.createCharacter("Wrong Hero")

        val exception = assertFailsWith<IllegalArgumentException> {
            reviewService.createReview(otherCharacterId, productId, orderItemId, 5, "Wrong owner")
        }
        assertTrue(exception.message!!.contains("does not belong to character"))
    }

    @Test
    fun testReviewRejectsUndeliveredOrderItem() = runBlocking {
        cartService.addToCart(characterId, productId, 1)
        val pendingOrderId = orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))
        val pendingDetails = orderService.getOrderDetailsOrNull(pendingOrderId)!!
        val pendingOrderItemId = pendingDetails.subOrders.first().items.first().id

        val exception = assertFailsWith<IllegalArgumentException> {
            reviewService.createReview(characterId, productId, pendingOrderItemId, 5, "Too early")
        }
        assertTrue(exception.message!!.contains("not eligible for review"))
    }

    @Test
    fun testReviewRejectsDuplicateReviewForSameOrderItem() = runBlocking {
        reviewService.createReview(characterId, productId, orderItemId, 5, "First review")

        val exception = assertFailsWith<IllegalArgumentException> {
            reviewService.createReview(characterId, productId, orderItemId, 4, "Second review")
        }
        assertTrue(exception.message!!.contains("Review already exists"))

        val reviews = reviewService.getProductReviews(productId)
        assertEquals(1, reviews.items.size)
    }
}
