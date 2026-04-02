package org.example.project.domain.cart

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.catalog.*
import org.example.project.domain.character.CharacterService
import org.example.project.domain.currency.CurrencyService
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class CartServiceTest {

    private lateinit var database: Database
    private lateinit var cartService: CartService
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private lateinit var characterService: CharacterService
    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.generateV7())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.generateV7())
    private var characterId: CharacterId = CharacterId(kotlin.uuid.Uuid.generateV7())
    private var productId: ProductId = ProductId(kotlin.uuid.Uuid.generateV7())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_cart_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        cartService = CartService(database)
        catalogService = CatalogService(database)
        currencyService = CurrencyService(database)
        characterService = CharacterService(database)
        runBlocking {
            goldId = currencyService.createCurrency("GOLD", "Gold", "G")
            merchantId = catalogService.createMerchant("Test Merchant")
            characterId = characterService.createCharacter("Test Hero")
            productId = catalogService.createProduct(
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
        }
    }

    @Test
    fun testAddToCartAndGetCart() = runBlocking {
        cartService.addToCart(characterId, productId, 2)
        val cart = cartService.getCart(characterId)
        assertEquals(1, cart.size)
        assertEquals(productId, cart.first().productId)
        assertEquals(2, cart.first().quantity)
    }

    @Test
    fun testAddToCartValidatesProduct(): Unit = runBlocking {
        val fakeProductId = ProductId(kotlin.uuid.Uuid.generateV7())
        assertFailsWith<IllegalArgumentException> {
            cartService.addToCart(characterId, fakeProductId, 1)
        }
    }

    @Test
    fun testUpdateQuantity() = runBlocking {
        val cartItemId = cartService.addToCart(characterId, productId, 1)
        cartService.updateQuantity(cartItemId, 5)
        val cart = cartService.getCart(characterId)
        assertEquals(5, cart.first().quantity)
    }

    @Test
    fun testRemoveFromCart() = runBlocking {
        val cartItemId = cartService.addToCart(characterId, productId, 1)
        cartService.removeFromCart(cartItemId)
        val cart = cartService.getCart(characterId)
        assertTrue(cart.isEmpty())
    }

    @Test
    fun testClearCart() = runBlocking {
        val armorId = catalogService.createProduct(
            Product.Armor(
                id = ProductId(kotlin.uuid.Uuid.generateV7()),
                name = "Test Shield",
                description = null,
                rarity = Rarity.COMMON,
                price = 50,
                currencyId = goldId,
                merchantId = merchantId,
                stock = 5,
                imageUrl = null,
                isActive = true,
                createdAt = kotlin.time.Instant.DISTANT_PAST,
                updatedAt = kotlin.time.Instant.DISTANT_PAST,
                defense = 3,
                armorSlot = ArmorSlot.SHIELD
            )
        )
        cartService.addToCart(characterId, productId, 1)
        cartService.addToCart(characterId, armorId, 1)
        cartService.clearCart(characterId)
        val cart = cartService.getCart(characterId)
        assertTrue(cart.isEmpty())
    }

    @Test
    fun testAddToCartRejectsCombinedQuantityAboveStock() = runBlocking {
        cartService.addToCart(characterId, productId, 8)

        val exception = assertFailsWith<IllegalArgumentException> {
            cartService.addToCart(characterId, productId, 3)
        }
        assertTrue(exception.message!!.contains("Insufficient stock"))

        val cart = cartService.getCart(characterId)
        assertEquals(8, cart.single().quantity)
    }

    @Test
    fun testUpdateQuantityRejectsQuantityAboveStock() = runBlocking {
        val cartItemId = cartService.addToCart(characterId, productId, 1)

        val exception = assertFailsWith<IllegalArgumentException> {
            cartService.updateQuantity(cartItemId, 11)
        }
        assertTrue(exception.message!!.contains("Insufficient stock"))

        val cart = cartService.getCart(characterId)
        assertEquals(1, cart.single().quantity)
    }
}
