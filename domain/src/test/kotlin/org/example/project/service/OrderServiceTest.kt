package org.example.project.service

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.domain.enums.DamageType
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.enums.Rarity
import org.example.project.domain.enums.WeaponSlot
import org.example.project.domain.id.*
import org.example.project.domain.model.Product
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class OrderServiceTest {
    private lateinit var database: Database
    private lateinit var orderService: OrderService
    private lateinit var cartService: CartService
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private lateinit var characterService: CharacterService
    private lateinit var shippingService: ShippingService

    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.random())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.random())
    private var characterId: CharacterId = CharacterId(kotlin.uuid.Uuid.random())
    private var shippingMethodId: ShippingMethodId = ShippingMethodId(kotlin.uuid.Uuid.random())
    private var swordId: ProductId = ProductId(kotlin.uuid.Uuid.random())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_order_", ".db").apply { deleteOnExit() }
        database = Database.connect("jdbc:sqlite:${testDbFile.absolutePath}").createTables()

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
            shippingMethodId = shippingService.createShippingMethod(
                name = "Courier Raven",
                baseCost = 50,
                currencyId = goldId,
                estimatedDays = 3
            )
            shippingService.addShippingMethodToMerchant(merchantId, shippingMethodId)

            swordId = catalogService.createProduct(
                Product.Weapon(
                    id = ProductId(kotlin.uuid.Uuid.random()),
                    name = "Test Sword",
                    description = null,
                    rarity = Rarity.COMMON,
                    price = 100,
                    currencyId = goldId,
                    merchantId = merchantId,
                    stock = 10,
                    imageUrl = null,
                    isActive = true,
                    createdAt = kotlin.time.Instant.DISTANT_PAST,
                    updatedAt = kotlin.time.Instant.DISTANT_PAST,
                    damage = 5,
                    damageType = DamageType.PHYSICAL,
                    weaponSlot = WeaponSlot.MAIN_HAND
                )
            )

            // Fund the character's wallet with enough gold
            characterService.deposit(characterId, goldId, 5000)
        }
    }

    @Test
    fun testCheckoutHappyPath() = runBlocking {
        cartService.addToCart(characterId, swordId, 2)

        val orderId = orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))

        // Verify: order exists with PENDING status
        val order = orderService.getOrder(orderId)
        assertNotNull(order)
        assertEquals(OrderStatus.PENDING, order.status)

        // Verify: order details has 1 suborder with 1 item (quantity 2, snapshotted price 100)
        val details = orderService.getOrderDetailsOrNull(orderId)
        assertNotNull(details)
        assertEquals(1, details.subOrders.size)
        val subOrderDetails = details.subOrders.first()
        assertEquals(1, subOrderDetails.items.size)
        val item = subOrderDetails.items.first()
        assertEquals(2, item.quantity)
        assertEquals(100L, item.snapshottedPrice)

        // Verify: wallet balance decreased by 250 (2*100 + 50 shipping)
        val balance = characterService.getWalletBalance(characterId)
        assertEquals(5000L - 250L, balance[goldId])

        // Verify: product stock decreased from 10 to 8
        val product = catalogService.getProduct(swordId)
        assertNotNull(product)
        assertEquals(8, product.stock)

        // Verify: cart is empty
        val cart = cartService.getCart(characterId)
        assertTrue(cart.isEmpty())
    }

    @Test
    fun testCheckoutEmptyCartFails() = runBlocking {
        val exception = assertFailsWith<IllegalArgumentException> {
            orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))
        }
        assertTrue(exception.message!!.contains("Cart is empty"))
    }

    @Test
    fun testCheckoutInsufficientBalanceFails() = runBlocking {
        // Create a new character with no gold
        val poorCharacterId = characterService.createCharacter("Poor Hero")

        cartService.addToCart(poorCharacterId, swordId, 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            orderService.checkout(poorCharacterId, mapOf(merchantId to shippingMethodId))
        }
        assertTrue(exception.message!!.contains("Insufficient wallet balance"))
    }

    @Test
    fun testCancelOrder() = runBlocking {
        cartService.addToCart(characterId, swordId, 1)
        val orderId = orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))

        // Cancel the order
        val cancelled = orderService.cancelOrder(orderId)
        assertTrue(cancelled)

        // Verify: order status is CANCELLED
        val order = orderService.getOrder(orderId)
        assertNotNull(order)
        assertEquals(OrderStatus.CANCELLED, order.status)

        // Verify: wallet balance is back to original (5000 - 150 + 150 = 5000)
        val balance = characterService.getWalletBalance(characterId)
        assertEquals(5000L, balance[goldId])

        // Verify: product stock is restored to 10
        val product = catalogService.getProduct(swordId)
        assertNotNull(product)
        assertEquals(10, product.stock)
    }

    @Test
    fun testGetOrderHistory() = runBlocking {
        // First order
        cartService.addToCart(characterId, swordId, 1)
        orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))

        // Second order
        cartService.addToCart(characterId, swordId, 1)
        orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))

        val history = orderService.getOrderHistory(characterId, offset = 0, limit = 10)
        assertEquals(2, history.items.size)
    }

    @Test
    fun testCancelNonPendingOrderFails() = runBlocking {
        cartService.addToCart(characterId, swordId, 1)
        val orderId = orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))

        // Cancel the order (succeeds)
        orderService.cancelOrder(orderId)

        // Try to cancel again - should fail because it's now CANCELLED
        val exception = assertFailsWith<IllegalArgumentException> {
            orderService.cancelOrder(orderId)
        }
        assertTrue(exception.message!!.contains("Can only cancel PENDING orders"))
    }
}
