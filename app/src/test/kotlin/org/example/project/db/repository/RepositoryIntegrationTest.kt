package org.example.project.db.repository

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.db.suspendTransaction
import org.example.project.domain.character.*
import org.example.project.domain.catalog.*
import org.example.project.domain.currency.*
import org.example.project.domain.order.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.ProductId
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant
import kotlin.test.*
import org.example.project.domain.character.CharacterRepository
import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.currency.CurrencyRepository

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class RepositoryIntegrationTest {

    private lateinit var database: Database
    private lateinit var characterRepo: CharacterRepository
    private lateinit var productRepo: ProductRepository
    private lateinit var merchantRepo: MerchantRepository
    private lateinit var currencyRepo: CurrencyRepository
    private lateinit var orderRepo: OrderRepository

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_repos_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        characterRepo = CharacterRepository()
        productRepo = ProductRepository()
        merchantRepo = MerchantRepository()
        currencyRepo = CurrencyRepository()
        orderRepo = OrderRepository()
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
            val originalTimestamp = Instant.fromEpochMilliseconds(1_000)
            val productId = Products.insertAndGetId {
                it[name] = "Repo Sword"
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 100
                it[currency] = goldId
                it[merchant] = mId
                it[stock] = 10
                it[Products.createdAt] = originalTimestamp
                it[Products.updatedAt] = originalTimestamp
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
            val sword = products.first() as Product.Weapon
            assertEquals("Repo Sword", sword.name)
            assertEquals(5, sword.damage)

            val originalUpdatedAt = sword.updatedAt
            val success = productRepo.updateStock(sword.id, -2)
            assertTrue(success)
            val updatedSword = productRepo.getProductOrNull(sword.id)
            assertNotNull(updatedSword)
            assertEquals(8, updatedSword.stock)
            assertTrue(updatedSword.updatedAt > originalUpdatedAt)
        }
    }

    @Test
    fun testOrderStatusRefreshesUpdatedAt() = runBlocking {
        database.suspendTransaction {
            val characterId = Characters.insertAndGetId {
                it[name] = "Repo Hero"
            }
            val currencyId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }
            val originalTimestamp = Instant.fromEpochMilliseconds(1_000)
            val orderId = OrderId(
                Orders.insertAndGetId {
                    it[character] = characterId
                    it[status] = OrderStatus.PENDING.name
                    it[totalPrice] = 250
                    it[totalCurrency] = currencyId
                    it[Orders.createdAt] = originalTimestamp
                    it[Orders.updatedAt] = originalTimestamp
                }.value
            )

            assertTrue(orderRepo.updateOrderStatus(orderId, OrderStatus.CANCELLED))

            val updatedOrder = orderRepo.getOrderOrNull(orderId)
            assertNotNull(updatedOrder)
            assertEquals(OrderStatus.CANCELLED, updatedOrder.status)
            assertTrue(updatedOrder.updatedAt > originalTimestamp)
        }
    }

    @Test
    fun testPotionDurationNullRoundTrip() = runBlocking {
        database.suspendTransaction {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }
            val merchantId = Merchants.insertAndGetId {
                it[name] = "Repo Alchemist"
            }

            val potion = Product.Potion(
                id = ProductId(kotlin.uuid.Uuid.generateV7()),
                name = "Null Duration Potion",
                description = null,
                rarity = Rarity.COMMON,
                price = 75,
                currencyId = CurrencyId(goldId.value),
                merchantId = MerchantId(merchantId.value),
                stock = 4,
                imageUrl = null,
                isActive = true,
                createdAt = Instant.DISTANT_PAST,
                updatedAt = Instant.DISTANT_PAST,
                effect = "Leaves duration unspecified",
                duration = null
            )

            val createdId = productRepo.createProduct(potion)
            val created = productRepo.getProductOrNull(createdId)
            assertNotNull(created)
            val createdPotion = created as Product.Potion
            assertNull(createdPotion.duration)

            val updated = createdPotion.copy(effect = "Still unspecified")
            assertTrue(productRepo.updateProduct(updated))
            val reloaded = productRepo.getProductOrNull(createdId)
            assertNotNull(reloaded)
            val reloadedPotion = reloaded as Product.Potion
            assertNull(reloadedPotion.duration)
        }
    }
}
