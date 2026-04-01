package org.example.project.domain.order

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.domain.cart.CartService
import org.example.project.domain.catalog.*
import org.example.project.domain.character.CharacterService
import org.example.project.domain.currency.CurrencyService
import org.example.project.domain.shipping.ShippingService
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.ByteBuffer
import java.sql.DriverManager
import kotlin.test.*
import kotlin.uuid.toJavaUuid

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class OrderServiceTest {
    private lateinit var database: Database
    private lateinit var databaseFile: java.io.File
    private lateinit var orderService: OrderService
    private lateinit var cartService: CartService
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private lateinit var characterService: CharacterService
    private lateinit var shippingService: ShippingService

    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.generateV7())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.generateV7())
    private var characterId: CharacterId = CharacterId(kotlin.uuid.Uuid.generateV7())
    private var shippingMethodId: ShippingMethodId = ShippingMethodId(kotlin.uuid.Uuid.generateV7())
    private var swordId: ProductId = ProductId(kotlin.uuid.Uuid.generateV7())

    @BeforeTest
    fun setup() {
        databaseFile = java.io.File.createTempFile("test_order_", ".db").apply { deleteOnExit() }
        database = Database.connect("jdbc:sqlite:${databaseFile.absolutePath}").createTables()

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
                    id = ProductId(kotlin.uuid.Uuid.generateV7()),
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

    private fun uuidBytes(id: kotlin.uuid.Uuid): ByteArray {
        val javaUuid = id.toJavaUuid()
        return ByteBuffer.allocate(16)
            .putLong(javaUuid.mostSignificantBits)
            .putLong(javaUuid.leastSignificantBits)
            .array()
    }

    private fun insertRawProduct(
        productId: ProductId,
        name: String,
        price: Long,
        stock: Int,
    ) {
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA ignore_check_constraints = ON")
            }
            connection.prepareStatement(
                """
                INSERT INTO products (id, name, category, rarity, price, currency_id, merchant_id, stock, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setBytes(1, uuidBytes(productId.value))
                statement.setString(2, name)
                statement.setString(3, ProductCategory.MISCELLANEOUS.name)
                statement.setString(4, Rarity.COMMON.name)
                statement.setLong(5, price)
                statement.setBytes(6, uuidBytes(goldId.value))
                statement.setBytes(7, uuidBytes(merchantId.value))
                statement.setInt(8, stock)
                statement.setBoolean(9, true)
                statement.executeUpdate()
            }
        }
    }

    private fun insertRawShippingMethod(
        shippingMethodId: ShippingMethodId,
        name: String,
        baseCost: Long,
        estimatedDays: Int = 3
    ) {
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA ignore_check_constraints = ON")
            }
            connection.prepareStatement(
                """
                INSERT INTO shipping_methods (id, name, description, base_cost, currency_id, estimated_days, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setBytes(1, uuidBytes(shippingMethodId.value))
                statement.setString(2, name)
                statement.setNull(3, java.sql.Types.VARCHAR)
                statement.setLong(4, baseCost)
                statement.setBytes(5, uuidBytes(goldId.value))
                statement.setInt(6, estimatedDays)
                statement.setBoolean(7, true)
                statement.executeUpdate()
            }
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
    fun testCheckoutRejectsNegativePersistedProductPrice() = runBlocking {
        val corruptedProductId = ProductId(kotlin.uuid.Uuid.generateV7())
        insertRawProduct(
            productId = corruptedProductId,
            name = "Corrupted Relic",
            price = -100,
            stock = 1
        )
        cartService.addToCart(characterId, corruptedProductId, 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))
        }
        assertTrue(exception.message!!.contains("Product price must be non-negative"))
    }

    @Test
    fun testCheckoutRejectsNegativePersistedShippingCost() = runBlocking {
        val safeProductId = ProductId(kotlin.uuid.Uuid.generateV7())
        insertRawProduct(
            productId = safeProductId,
            name = "Legit Trinket",
            price = 100,
            stock = 1
        )
        cartService.addToCart(characterId, safeProductId, 1)

        val corruptedShippingMethodId = ShippingMethodId(kotlin.uuid.Uuid.generateV7())
        insertRawShippingMethod(
            shippingMethodId = corruptedShippingMethodId,
            name = "Corrupted Courier",
            baseCost = -50
        )
        shippingService.addShippingMethodToMerchant(merchantId, corruptedShippingMethodId)

        val exception = assertFailsWith<IllegalArgumentException> {
            orderService.checkout(characterId, mapOf(merchantId to corruptedShippingMethodId))
        }
        assertTrue(exception.message!!.contains("Shipping base cost must be non-negative"))
    }

    @Test
    fun testCheckoutMixedCurrencyCartUsesDeterministicSettlementCurrency() = runBlocking {
        val silverId = currencyService.createCurrency("SILVER", "Silver", "S")
        currencyService.setConversionRate(goldId, silverId, 10.0)

        val silverTrinketId = catalogService.createProduct(
            Product.MiscItem(
                id = ProductId(kotlin.uuid.Uuid.generateV7()),
                name = "Silver Trinket",
                description = null,
                rarity = Rarity.COMMON,
                price = 100,
                currencyId = silverId,
                merchantId = merchantId,
                stock = 10,
                imageUrl = null,
                isActive = true,
                createdAt = kotlin.time.Instant.DISTANT_PAST,
                updatedAt = kotlin.time.Instant.DISTANT_PAST
            )
        )
        val silverShippingMethodId = shippingService.createShippingMethod(
            name = "Silver Courier",
            baseCost = 100,
            currencyId = silverId,
            estimatedDays = 2
        )
        shippingService.addShippingMethodToMerchant(merchantId, silverShippingMethodId)

        cartService.addToCart(characterId, silverTrinketId, 1)
        cartService.addToCart(characterId, swordId, 1)

        val orderId = orderService.checkout(characterId, mapOf(merchantId to silverShippingMethodId))

        val order = orderService.getOrder(orderId)
        assertNotNull(order)
        assertEquals(goldId, order.totalCurrencyId)
        assertEquals(120L, order.totalPrice)

        val details = orderService.getOrderDetailsOrNull(orderId)
        assertNotNull(details)
        assertEquals(1, details.subOrders.size)

        val subOrder = details.subOrders.single().subOrder
        assertEquals(10L, subOrder.shippingCost)
        assertEquals(120L, subOrder.merchantTotalPrice)

        val itemCurrencies = details.subOrders.single().items.map { it.snapshottedCurrencyId }.toSet()
        assertEquals(setOf(goldId, silverId), itemCurrencies)

        val balance = characterService.getWalletBalance(characterId)
        assertEquals(4880L, balance[goldId])

        val goldProduct = catalogService.getProduct(swordId)
        assertNotNull(goldProduct)
        assertEquals(9, goldProduct.stock)

        val silverProduct = catalogService.getProduct(silverTrinketId)
        assertNotNull(silverProduct)
        assertEquals(9, silverProduct.stock)

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
    fun testCheckoutRejectsUnlinkedShippingMethod() = runBlocking {
        val otherMerchantId = catalogService.createMerchant("Other Merchant")
        val otherShippingMethodId = shippingService.createShippingMethod(
            name = "Other Courier",
            baseCost = 75,
            currencyId = goldId,
            estimatedDays = 5
        )
        shippingService.addShippingMethodToMerchant(otherMerchantId, otherShippingMethodId)

        cartService.addToCart(characterId, swordId, 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            orderService.checkout(characterId, mapOf(merchantId to otherShippingMethodId))
        }
        assertTrue(exception.message!!.contains("is not available for merchant"))
    }

    @Test
    fun testCheckoutRejectsInactiveShippingMethod() = runBlocking {
        shippingService.updateShippingMethod(shippingMethodId, isActive = false)

        cartService.addToCart(characterId, swordId, 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            orderService.checkout(characterId, mapOf(merchantId to shippingMethodId))
        }
        assertTrue(exception.message!!.contains("Shipping method is not active"))
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
