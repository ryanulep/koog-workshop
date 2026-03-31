package org.example.project.db.repository

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.db.suspendTransaction
import org.example.project.db.tables.Currencies
import org.example.project.db.tables.Merchants
import org.example.project.db.tables.Products
import org.example.project.db.tables.Weapons
import org.example.project.db.tables.Weapons.damage
import org.example.project.db.tables.Weapons.damageType
import org.example.project.db.tables.Weapons.weaponSlot
import org.example.project.domain.enums.DamageType
import org.jetbrains.exposed.v1.jdbc.Database
import org.example.project.domain.enums.ProductCategory
import org.example.project.domain.enums.Rarity
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.enums.WeaponSlot
import org.example.project.domain.id.CurrencyId
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class RepositoryIntegrationTest {

    private lateinit var database: Database
    private lateinit var characterRepo: ExposedCharacterRepository
    private lateinit var productRepo: ExposedProductRepository
    private lateinit var merchantRepo: ExposedMerchantRepository
    private lateinit var currencyRepo: ExposedCurrencyRepository

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_repos_", ".db").apply { deleteOnExit() }
        database = Database.connect("jdbc:sqlite:${testDbFile.absolutePath}").createTables()
        characterRepo = ExposedCharacterRepository()
        productRepo = ExposedProductRepository()
        merchantRepo = ExposedMerchantRepository()
        currencyRepo = ExposedCurrencyRepository()
    }

    @Test
    fun testCharacterAndWallet() = runBlocking {
        val currencyId = CurrencyId(transaction(database) {
            Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }.value
        })

        database.suspendTransaction {
            val charId = characterRepo.createCharacter("Test Aldric")
            val character = characterRepo.getCharacterOrNull(charId)
            assertNotNull(character)
            assertEquals("Test Aldric", character.name)

            val balance = characterRepo.getWalletBalance(charId)
            assertTrue(balance.isEmpty())

            characterRepo.addTransaction(charId, currencyId, 1000, TransactionType.DEPOSIT)
            characterRepo.addTransaction(charId, currencyId, -200, TransactionType.PURCHASE)

            val newBalance = characterRepo.getWalletBalance(charId)
            assertEquals(800L, newBalance[currencyId])
        }
    }

    @Test
    fun testProductMapping() = runBlocking {
        // We need to seed some data first since we are in memory
        database.suspendTransaction {
            // Currencies and Merchants needed for Products
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }
            val mId = Merchants.insertAndGetId {
                it[name] = "Repo Merchant"
            }
            val productId = Products.insertAndGetId {
                it[name] = "Repo Sword"
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 100
                it[currency] = goldId
                it[merchant] = mId
                it[stock] = 10
            }
            Weapons.insert {
                it[id] = productId
                it[damage] = 5
                it[damageType] = DamageType.PHYSICAL.name
                it[weaponSlot] = WeaponSlot.MAIN_HAND.name
            }
        }

        database.suspendTransaction {
            val products = productRepo.getAllProducts()
            assertEquals(1, products.size)
            val sword = products.first() as org.example.project.domain.model.Product.Weapon
            assertEquals("Repo Sword", sword.name)
            assertEquals(5, sword.damage)

            val success = productRepo.updateStock(sword.id, -2)
            assertTrue(success)
            val updatedSword = productRepo.getProductOrNull(sword.id)
            assertEquals(8, updatedSword?.stock)
        }
    }
}
