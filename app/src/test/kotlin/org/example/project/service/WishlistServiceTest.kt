package org.example.project.domain.wishlist

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
class WishlistServiceTest {

    private lateinit var database: Database
    private lateinit var wishlistService: WishlistService
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private lateinit var characterService: CharacterService
    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.generateV7())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.generateV7())
    private var characterId: CharacterId = CharacterId(kotlin.uuid.Uuid.generateV7())
    private var productId: ProductId = ProductId(kotlin.uuid.Uuid.generateV7())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_wishlist_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        wishlistService = WishlistService(database)
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
    fun testAddAndGetWishlist() = runBlocking {
        wishlistService.addToWishlist(characterId, productId)
        val wishlist = wishlistService.getWishlist(characterId)
        assertEquals(1, wishlist.size)
        assertEquals(productId, wishlist.first().productId)
    }

    @Test
    fun testAddToWishlistValidatesProduct(): Unit = runBlocking {
        val fakeProductId = ProductId(kotlin.uuid.Uuid.generateV7())
        assertFailsWith<IllegalArgumentException> {
            wishlistService.addToWishlist(characterId, fakeProductId)
        }
    }

    @Test
    fun testRemoveFromWishlist() = runBlocking {
        val wishlistItemId = wishlistService.addToWishlist(characterId, productId)
        wishlistService.removeFromWishlist(wishlistItemId)
        val wishlist = wishlistService.getWishlist(characterId)
        assertTrue(wishlist.isEmpty())
    }

    @Test
    fun testClearWishlist() = runBlocking {
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
        wishlistService.addToWishlist(characterId, productId)
        wishlistService.addToWishlist(characterId, armorId)
        wishlistService.clearWishlist(characterId)
        val wishlist = wishlistService.getWishlist(characterId)
        assertTrue(wishlist.isEmpty())
    }

    @Test
    fun testAddDuplicateReturnsExistingId() = runBlocking {
        val firstId = wishlistService.addToWishlist(characterId, productId)
        val secondId = wishlistService.addToWishlist(characterId, productId)
        assertEquals(firstId, secondId)
    }
}
