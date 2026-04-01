package org.example.project.service

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.domain.enums.DamageType
import org.example.project.domain.enums.Rarity
import org.example.project.domain.enums.WeaponSlot
import org.example.project.domain.id.*
import org.example.project.domain.model.Product
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ReviewServiceTest {
    private lateinit var database: Database
    private lateinit var reviewService: ReviewService
    private lateinit var orderService: OrderService
    private lateinit var cartService: CartService
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private lateinit var characterService: CharacterService
    private lateinit var shippingService: ShippingService

    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.random())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.random())
    private var characterId: CharacterId = CharacterId(kotlin.uuid.Uuid.random())
    private var productId: ProductId = ProductId(kotlin.uuid.Uuid.random())
    private var orderItemId: OrderItemId = OrderItemId(kotlin.uuid.Uuid.random())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_review_", ".db").apply { deleteOnExit() }
        database = Database.connect("jdbc:sqlite:${testDbFile.absolutePath}").createTables()

        reviewService = ReviewService(database)
        orderService = OrderService(database)
        cartService = CartService(database)
        catalogService = CatalogService(database)
        currencyService = CurrencyService(database)
        characterService = CharacterService(database)
        shippingService = ShippingService(database)

        runBlocking {
            goldId = currencyService.createCurrency("GOLD", "Gold", "G")
            merchantId = catalogService.createMerchant("Test Merchant")
            characterId = characterService.createCharacter("Test Hero")
            val shippingMethodId = shippingService.createShippingMethod(
                name = "Courier Raven", baseCost = 50, currencyId = goldId, estimatedDays = 3
            )
            shippingService.addShippingMethodToMerchant(merchantId, shippingMethodId)

            productId = catalogService.createProduct(
                Product.Weapon(
                    id = ProductId(kotlin.uuid.Uuid.random()),
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
}
