package org.example.project.db.repository

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.jetbrains.exposed.v1.jdbc.Database
import org.example.project.domain.enums.ProductCategory
import org.example.project.domain.enums.TransactionType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class RepositoryIntegrationTest {

    private lateinit var database: Database
    private lateinit var characterRepo: CharacterRepository
    private lateinit var productRepo: ProductRepository
    private lateinit var merchantRepo: MerchantRepository
    private lateinit var currencyRepo: CurrencyRepository

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_repos_", ".db").apply { deleteOnExit() }
        database = Database.connect("jdbc:sqlite:${testDbFile.absolutePath}").createTables()
        characterRepo = CharacterRepository(database)
        productRepo = ProductRepository(database)
        merchantRepo = MerchantRepository(database)
        currencyRepo = CurrencyRepository(database)
    }

    @Test
    fun testCharacterAndWallet() = runBlocking {
        // Seed currency first
        val currencyId = transaction(database) {
            org.example.project.db.tables.Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }.value
        }

        val charId = characterRepo.createCharacter("Test Aldric")
        val character = characterRepo.getCharacter(charId)
        assertNotNull(character)
        assertEquals("Test Aldric", character.name)

        // Initial balance should be empty
        val balance = characterRepo.getWalletBalance(charId)
        assertTrue(balance.isEmpty())

        // Add some transactions
        characterRepo.addTransaction(charId, currencyId, 1000, TransactionType.DEPOSIT)
        characterRepo.addTransaction(charId, currencyId, -200, TransactionType.PURCHASE)

        val newBalance = characterRepo.getWalletBalance(charId)
        assertEquals(800L, newBalance[currencyId])
    }

    @Test
    fun testProductMapping() = runBlocking {
        // We need to seed some data first since we are in memory
        transaction(database) {
            // Currencies and Merchants needed for Products
            val goldId = org.example.project.db.tables.Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }
            val mId = org.example.project.db.tables.Merchants.insertAndGetId {
                it[name] = "Repo Merchant"
            }
            org.example.project.db.tables.Products.insert {
                it[name] = "Repo Sword"
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = org.example.project.domain.enums.Rarity.COMMON.name
                it[price] = 100
                it[currency] = goldId
                it[merchant] = mId
                it[stock] = 10
                it[damage] = 5
                it[damageType] = org.example.project.domain.enums.DamageType.PHYSICAL.name
                it[weaponSlot] = org.example.project.domain.enums.WeaponSlot.MAIN_HAND.name
            }
        }

        val products = productRepo.getAllProducts()
        assertEquals(1, products.size)
        val sword = products.first() as org.example.project.domain.model.Product.Weapon
        assertEquals("Repo Sword", sword.name)
        assertEquals(5, sword.damage)

        // Test stock update
        val success = productRepo.updateStock(sword.id, -2)
        assertTrue(success)
        val updatedSword = productRepo.getProductById(sword.id)
        assertEquals(8, updatedSword?.stock)
    }
}
