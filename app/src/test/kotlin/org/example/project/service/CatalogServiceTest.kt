package org.example.project.domain.catalog

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.currency.CurrencyService
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class CatalogServiceTest {
    private lateinit var database: Database
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.generateV7())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.generateV7())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_catalog_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        currencyService = CurrencyService(database)
        catalogService = CatalogService(database)
        runBlocking {
            goldId = currencyService.createCurrency("GOLD", "Gold", "G")
            merchantId = catalogService.createMerchant("Test Merchant")
        }
    }

    private fun createWeapon(
        name: String = "Test Sword",
        price: Long = 100,
        stock: Int = 10
    ) = Product.Weapon(
        id = ProductId(kotlin.uuid.Uuid.generateV7()),
        name = name,
        description = null,
        rarity = Rarity.COMMON,
        price = price,
        currencyId = goldId,
        merchantId = merchantId,
        stock = stock,
        imageUrl = null,
        isActive = true,
        createdAt = kotlin.time.Instant.DISTANT_PAST,
        updatedAt = kotlin.time.Instant.DISTANT_PAST,
        damage = 5,
        damageType = DamageType.PHYSICAL,
        weaponSlot = WeaponSlot.MAIN_HAND
    )

    private fun createPotion(name: String = "Health Potion") = Product.Potion(
        id = ProductId(kotlin.uuid.Uuid.generateV7()),
        name = name,
        description = null,
        rarity = Rarity.COMMON,
        price = 50,
        currencyId = goldId,
        merchantId = merchantId,
        stock = 20,
        imageUrl = null,
        isActive = true,
        createdAt = kotlin.time.Instant.DISTANT_PAST,
        updatedAt = kotlin.time.Instant.DISTANT_PAST,
        effect = "Restores health",
        duration = 10
    )

    @Test
    fun testCreateAndGetProduct() = runBlocking {
        val weapon = createWeapon()
        val id = catalogService.createProduct(weapon)
        val product = catalogService.getProductOrNull(id)
        assertNotNull(product)
        assertIs<Product.Weapon>(product)
        assertEquals("Test Sword", product.name)
        assertEquals(5, product.damage)
        assertEquals(DamageType.PHYSICAL, product.damageType)
        assertEquals(WeaponSlot.MAIN_HAND, product.weaponSlot)
    }

    @Test
    fun testGetProductsByCategory() = runBlocking {
        catalogService.createProduct(createWeapon())
        catalogService.createProduct(createPotion())
        val weapons = catalogService.getProductsByCategory(ProductCategory.WEAPONS)
        assertEquals(1, weapons.size)
    }

    @Test
    fun testUpdateProduct() = runBlocking {
        val weapon = createWeapon()
        val id = catalogService.createProduct(weapon)
        val created = catalogService.getProductOrNull(id)
        assertNotNull(created)
        val createdWeapon = created as Product.Weapon
        val updated = catalogService.updateProduct(createdWeapon.copy(name = "Epic Sword"))
        assertTrue(updated)
        val fetched = catalogService.getProductOrNull(id)
        assertNotNull(fetched)
        assertEquals("Epic Sword", fetched.name)
    }

    @Test
    fun testCreateProductRejectsNegativeValues() = runBlocking {
        val negativePriceException = assertFailsWith<IllegalArgumentException> {
            catalogService.createProduct(createWeapon(price = -1))
        }
        assertTrue(negativePriceException.message!!.contains("non-negative"))

        val negativeStockException = assertFailsWith<IllegalArgumentException> {
            catalogService.createProduct(createWeapon(stock = -1))
        }
        assertTrue(negativeStockException.message!!.contains("non-negative"))
    }

    @Test
    fun testUpdateProductRejectsNegativeValues() = runBlocking {
        val id = catalogService.createProduct(createWeapon())
        val created = catalogService.getProductOrNull(id)
        assertNotNull(created)
        val createdWeapon = created as Product.Weapon

        val negativePriceException = assertFailsWith<IllegalArgumentException> {
            catalogService.updateProduct(createdWeapon.copy(price = -1))
        }
        assertTrue(negativePriceException.message!!.contains("non-negative"))

        val negativeStockException = assertFailsWith<IllegalArgumentException> {
            catalogService.updateProduct(createdWeapon.copy(stock = -1))
        }
        assertTrue(negativeStockException.message!!.contains("non-negative"))
    }

    @Test
    fun testDeleteProduct() = runBlocking {
        val id = catalogService.createProduct(createWeapon())
        val deleted = catalogService.deleteProduct(id)
        assertTrue(deleted)
        val product = catalogService.getProductOrNull(id)
        assertNull(product)
    }

    @Test
    fun testCreateAndGetMerchant() = runBlocking {
        val id = catalogService.createMerchant("Magic Shop", description = "Sells magical items")
        val merchant = catalogService.getMerchantOrNull(id)
        assertNotNull(merchant)
        assertEquals("Magic Shop", merchant.name)
        assertEquals("Sells magical items", merchant.description)
    }

    @Test
    fun testUpdateMerchant() = runBlocking {
        val id = catalogService.createMerchant("Magic Shop")
        val updated = catalogService.updateMerchant(id, name = "Grand Magic Shop")
        assertTrue(updated)
        val merchant = catalogService.getMerchantOrNull(id)
        assertNotNull(merchant)
        assertEquals("Grand Magic Shop", merchant.name)
    }

    @Test
    fun testDeleteMerchant() = runBlocking {
        val id = catalogService.createMerchant("Magic Shop")
        val deleted = catalogService.deleteMerchant(id)
        assertTrue(deleted)
        val merchant = catalogService.getMerchantOrNull(id)
        assertNull(merchant)
    }

    @Test
    fun testGetMerchantsPagination() = runBlocking {
        catalogService.createMerchant("Shop A")
        catalogService.createMerchant("Shop B")
        catalogService.createMerchant("Shop C")
        val page = catalogService.getMerchants(offset = 0, limit = 2)
        assertEquals(2, page.items.size)
        assertEquals(4L, page.total) // 3 created + 1 from setup
    }
}
