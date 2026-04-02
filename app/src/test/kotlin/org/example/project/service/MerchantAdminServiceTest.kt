package org.example.project.domain.admin

import kotlinx.coroutines.runBlocking
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
class MerchantAdminServiceTest {

    @Test
    fun `loadMerchants and detail expose counts and assignments`() = runBlocking {
        val database = createDatabase()
        val fixture = seedMerchants(database)
        val service = MerchantAdminService(database)

        val merchants = service.loadMerchants()
        val detail = service.loadMerchantDetailOrNull(fixture.blackforgeMerchantId)

        assertEquals(listOf("Blackforge Armory", "Moonwell Remedies"), merchants.map { merchant -> merchant.name })
        assertEquals(2, merchants.first().productCount)
        assertEquals(1, merchants.first().recentOrderCount)

        assertNotNull(detail)
        assertEquals("Blackforge Armory", detail.merchant.name)
        assertEquals(listOf(fixture.ravenShippingId), detail.assignedShippingMethods.map { it.id })
    }

    @Test
    fun `merchant mutations update active state and assignments`() = runBlocking {
        val database = createDatabase()
        val fixture = seedMerchants(database)
        val service = MerchantAdminService(database)

        assertTrue(service.setMerchantActive(fixture.blackforgeMerchantId, false))
        assertTrue(service.setShippingMethodActive(fixture.portalShippingId, false))
        assertTrue(
            service.replaceMerchantShippingMethods(
                merchantId = fixture.blackforgeMerchantId,
                shippingMethodIds = setOf(fixture.portalShippingId)
            )
        )

        val detail = service.loadMerchantDetailOrNull(fixture.blackforgeMerchantId)

        assertNotNull(detail)
        assertFalse(detail.merchant.isActive)
        assertEquals(setOf(fixture.portalShippingId), detail.assignedShippingMethods.map { it.id }.toSet())
        assertFalse(
            detail.availableShippingMethods.single { shippingMethod ->
                shippingMethod.id == fixture.portalShippingId
            }.isActive
        )
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("merchant_admin_", ".db").apply {
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
                ravenShippingId = ShippingMethodId(ravenId.value),
                portalShippingId = ShippingMethodId(portalId.value)
            )
        }

    private data class MerchantFixture(
        val blackforgeMerchantId: MerchantId,
        val ravenShippingId: ShippingMethodId,
        val portalShippingId: ShippingMethodId
    )
}
