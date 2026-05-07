package org.example.project.admin

import org.example.project.admin.merchants.AdminMerchantRepository
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.character.Characters
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.Orders
import org.example.project.domain.order.SubOrders
import org.example.project.domain.shipping.MerchantShippingMethods
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.shipping.ShippingRepository
import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class AdminMerchantRepositoryTest {

    @Test
    fun `merchant list maps product counts and recent order counts`() {
        val database = createDatabase()
        val fixture = seedMerchants(database)

        transaction(database) {
            val merchants = AdminMerchantRepository().getMerchants()

            assertEquals(
                listOf(fixture.blackforgeMerchantId, fixture.moonwellMerchantId),
                merchants.map { merchant -> merchant.id }
            )

            val blackforge = merchants.first()
            assertEquals("Blackforge Armory", blackforge.name)
            assertEquals("North Ward", blackforge.location)
            assertEquals(2, blackforge.productCount)
            assertEquals(1, blackforge.recentOrderCount)

            val moonwell = merchants.last()
            assertEquals(1, moonwell.productCount)
            assertEquals(0, moonwell.recentOrderCount)
        }
    }

    @Test
    fun `merchant detail maps shipping assignments and shipping metadata`() {
        val database = createDatabase()
        val fixture = seedMerchants(database)

        transaction(database) {
            val detail = AdminMerchantRepository().getMerchantDetailOrNull(fixture.blackforgeMerchantId)

            assertNotNull(detail)
            assertEquals("Blackforge Armory", detail.merchant.name)
            assertEquals(2, detail.productCount)
            assertEquals(1, detail.recentOrderCount)
            assertEquals(listOf(fixture.ravenShippingId), detail.assignedShippingMethods.map { it.id })

            val availableShippingMethods = detail.availableShippingMethods.associateBy { it.id }
            assertTrue(availableShippingMethods.getValue(fixture.ravenShippingId).isAssigned)
            assertFalse(availableShippingMethods.getValue(fixture.portalShippingId).isAssigned)
            assertEquals("GOLD", availableShippingMethods.getValue(fixture.portalShippingId).currencyCode)
        }
    }

    @Test
    fun `merchant and shipping writes refresh repository projections`() {
        val database = createDatabase()
        val fixture = seedMerchants(database)

        transaction(database) {
            val merchantRepository = MerchantRepository()
            val shippingRepository = ShippingRepository()
            val adminMerchantRepository = AdminMerchantRepository()

            assertTrue(merchantRepository.setMerchantActive(fixture.blackforgeMerchantId, false))
            assertTrue(shippingRepository.setShippingMethodActive(fixture.portalShippingId, false))

            shippingRepository.replaceMerchantShippingMethods(
                merchantId = fixture.blackforgeMerchantId,
                shippingMethodIds = setOf(fixture.portalShippingId)
            )

            val detail = adminMerchantRepository.getMerchantDetailOrNull(fixture.blackforgeMerchantId)
            assertNotNull(detail)
            assertFalse(detail.merchant.isActive)
            assertEquals(listOf(fixture.portalShippingId), detail.assignedShippingMethods.map { it.id })
            assertFalse(
                detail.availableShippingMethods.single { shippingMethod ->
                    shippingMethod.id == fixture.portalShippingId
                }.isActive
            )
        }
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("admin_merchant_repo_", ".db").apply {
            deleteOnExit()
        }
        return connectSqlite(databaseFile).createTables()
    }

    private fun seedMerchants(database: Database): MerchantFixture =
        transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }

            val blackforgeId = Merchants.insertAndGetId {
                it[name] = "Blackforge Armory"
                it[description] = "Forged steel and field-ready armor."
                it[location] = "North Ward"
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(1_000)
            }
            val moonwellId = Merchants.insertAndGetId {
                it[name] = "Moonwell Remedies"
                it[description] = "Alchemical draughts and restorative tonics."
                it[location] = "Canal Market"
                it[createdAt] = Instant.fromEpochMilliseconds(2_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }

            Products.insert {
                it[name] = "Aether Blade"
                it[description] = "Balanced steel for wardens."
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.UNCOMMON.name
                it[price] = 320
                it[currency] = goldId
                it[merchant] = blackforgeId
                it[stock] = 12
            }
            Products.insert {
                it[name] = "Bastion Shield"
                it[description] = "Layered oak and steel."
                it[category] = ProductCategory.ARMOR.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 275
                it[currency] = goldId
                it[merchant] = blackforgeId
                it[stock] = 4
            }
            Products.insert {
                it[name] = "Moonwell Draught"
                it[description] = "A cooling tonic."
                it[category] = ProductCategory.POTIONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 90
                it[currency] = goldId
                it[merchant] = moonwellId
                it[stock] = 6
            }

            val ravenId = ShippingMethods.insertAndGetId {
                it[name] = "Courier Raven"
                it[description] = "Fast local courier."
                it[baseCost] = 20
                it[currency] = goldId
                it[estimatedDays] = 2
                it[createdAt] = Instant.fromEpochMilliseconds(3_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
            }
            val portalId = ShippingMethods.insertAndGetId {
                it[name] = "Portal Relay"
                it[description] = "Premium relay lane."
                it[baseCost] = 35
                it[currency] = goldId
                it[estimatedDays] = 1
                it[createdAt] = Instant.fromEpochMilliseconds(4_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(4_000)
            }

            MerchantShippingMethods.insert {
                it[merchant] = blackforgeId
                it[shippingMethod] = ravenId
            }
            MerchantShippingMethods.insert {
                it[merchant] = moonwellId
                it[shippingMethod] = portalId
            }

            val characterId = Characters.insertAndGetId {
                it[name] = "Aldric"
            }
            val latestCreatedAt = 60.days.inWholeMilliseconds
            val olderCreatedAt = 20.days.inWholeMilliseconds

            val recentOrderId = Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.CONFIRMED.name
                it[totalPrice] = 340
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(latestCreatedAt)
                it[updatedAt] = Instant.fromEpochMilliseconds(latestCreatedAt)
            }
            val olderOrderId = Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.DELIVERED.name
                it[totalPrice] = 105
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(olderCreatedAt)
                it[updatedAt] = Instant.fromEpochMilliseconds(olderCreatedAt)
            }

            SubOrders.insertAndGetId {
                it[order] = recentOrderId
                it[merchant] = blackforgeId
                it[status] = OrderStatus.SHIPPED.name
                it[shippingMethod] = ravenId
                it[shippingCost] = 20
                it[merchantTotalPrice] = 340
                it[createdAt] = Instant.fromEpochMilliseconds(latestCreatedAt)
                it[updatedAt] = Instant.fromEpochMilliseconds(latestCreatedAt)
            }
            SubOrders.insertAndGetId {
                it[order] = olderOrderId
                it[merchant] = moonwellId
                it[status] = OrderStatus.DELIVERED.name
                it[shippingMethod] = portalId
                it[shippingCost] = 15
                it[merchantTotalPrice] = 105
                it[createdAt] = Instant.fromEpochMilliseconds(olderCreatedAt)
                it[updatedAt] = Instant.fromEpochMilliseconds(olderCreatedAt)
            }

            MerchantFixture(
                blackforgeMerchantId = MerchantId(blackforgeId.value),
                moonwellMerchantId = MerchantId(moonwellId.value),
                ravenShippingId = ShippingMethodId(ravenId.value),
                portalShippingId = ShippingMethodId(portalId.value)
            )
        }

    private data class MerchantFixture(
        val blackforgeMerchantId: MerchantId,
        val moonwellMerchantId: MerchantId,
        val ravenShippingId: ShippingMethodId,
        val portalShippingId: ShippingMethodId
    )
}
