package org.example.project.domain.admin.products

import kotlinx.coroutines.runBlocking
import org.example.project.admin.products.AdminProductService
import org.example.project.admin.products.ProductActiveFilter
import org.example.project.admin.products.ProductFilter
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Potions
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.catalog.Weapons
import org.example.project.domain.character.Characters
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.OrderItems
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.Orders
import org.example.project.domain.order.SubOrders
import org.example.project.domain.review.Reviews
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ProductServiceTest {

    @Test
    fun `loadProducts applies filters and exposes review summary`() = runBlocking {
        val database = createDatabase()
        val fixture = seedProducts(database)
        val service = AdminProductService(database)

        val merchantProducts = service.loadProducts(
            ProductFilter(
                merchantId = fixture.blackforgeMerchantId,
                activeFilter = ProductActiveFilter.ACTIVE
            )
        )

        assertEquals(listOf(fixture.bronzeBladeId), merchantProducts.map { it.id })
        assertEquals(1, merchantProducts.single().reviewSummary.reviewCount)
        assertEquals(4.0, merchantProducts.single().reviewSummary.averageRating)

        val inactiveProducts = service.loadProducts(
            ProductFilter(
                nameQuery = "draught",
                activeFilter = ProductActiveFilter.INACTIVE
            )
        )

        assertEquals(listOf(fixture.moonwellDraughtId), inactiveProducts.map { it.id })
    }

    @Test
    fun `product mutations are reflected in detail`() = runBlocking {
        val database = createDatabase()
        val fixture = seedProducts(database)
        val service = AdminProductService(database)

        assertTrue(service.adjustStock(fixture.bronzeBladeId, -2))
        assertTrue(service.setProductActive(fixture.bronzeBladeId, false))

        val detail = service.loadProductDetailOrNull(fixture.bronzeBladeId)

        assertNotNull(detail)
        assertEquals(10, detail.stock)
        assertFalse(detail.isActive)
        assertTrue(
            detail.categoryAttributes.any { attribute ->
                attribute.label == "Damage" && attribute.value == "14"
            }
        )
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("product_admin_", ".db").apply {
            deleteOnExit()
        }
        return connectSqlite(databaseFile).createTables()
    }

    private fun seedProducts(database: Database): ProductFixture =
        transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }

            val blackforgeId = Merchants.insertAndGetId {
                it[name] = "Blackforge Armory"
            }
            val moonwellId = Merchants.insertAndGetId {
                it[name] = "Moonwell Remedies"
            }

            val bronzeBladeId = Products.insertAndGetId {
                it[name] = "Bronze Blade"
                it[description] = "A dependable forged short sword."
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.UNCOMMON.name
                it[price] = 320
                it[currency] = goldId
                it[merchant] = blackforgeId
                it[stock] = 12
                it[createdAt] = Instant.fromEpochMilliseconds(2_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }
            Weapons.insert {
                it[id] = bronzeBladeId
                it[damage] = 14
                it[damageType] = org.example.project.domain.catalog.DamageType.PHYSICAL.name
                it[weaponSlot] = org.example.project.domain.catalog.WeaponSlot.MAIN_HAND.name
            }

            val moonwellDraughtId = Products.insertAndGetId {
                it[name] = "Moonwell Draught"
                it[description] = "A cooling potion for exhausted scouts."
                it[category] = ProductCategory.POTIONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 90
                it[currency] = goldId
                it[merchant] = moonwellId
                it[stock] = 6
                it[isActive] = false
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(1_000)
            }
            Potions.insert {
                it[id] = moonwellDraughtId
                it[effect] = "Restore stamina"
                it[duration] = 3
            }

            val characterId = Characters.insertAndGetId {
                it[name] = "Aldric"
            }
            val orderId = Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.DELIVERED.name
                it[totalPrice] = 320
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(3_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
            }
            val subOrderId = SubOrders.insertAndGetId {
                it[order] = orderId
                it[merchant] = blackforgeId
                it[status] = OrderStatus.DELIVERED.name
                it[shippingMethod] = seedShippingMethod(goldId)
                it[shippingCost] = 15
                it[merchantTotalPrice] = 335
                it[createdAt] = Instant.fromEpochMilliseconds(3_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
            }
            val orderItemId = OrderItems.insertAndGetId {
                it[subOrder] = subOrderId
                it[product] = bronzeBladeId
                it[quantity] = 1
                it[snapshottedPrice] = 320
                it[snapshottedCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(3_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
            }
            Reviews.insertAndGetId {
                it[character] = characterId
                it[product] = bronzeBladeId
                it[orderItem] = orderItemId
                it[rating] = 4
                it[text] = "Reliable steel."
                it[createdAt] = Instant.fromEpochMilliseconds(4_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(4_000)
            }

            ProductFixture(
                bronzeBladeId = org.example.project.domain.shared.ProductId(bronzeBladeId.value),
                moonwellDraughtId = org.example.project.domain.shared.ProductId(moonwellDraughtId.value),
                blackforgeMerchantId = org.example.project.domain.shared.MerchantId(blackforgeId.value)
            )
        }

    private fun org.jetbrains.exposed.v1.core.Transaction.seedShippingMethod(
        currencyId: org.jetbrains.exposed.v1.core.dao.id.EntityID<kotlin.uuid.Uuid>
    ) = org.example.project.domain.shipping.ShippingMethods.insertAndGetId {
        it[name] = "Courier Raven"
        it[baseCost] = 15
        it[currency] = currencyId
        it[estimatedDays] = 2
    }

    private data class ProductFixture(
        val bronzeBladeId: org.example.project.domain.shared.ProductId,
        val moonwellDraughtId: org.example.project.domain.shared.ProductId,
        val blackforgeMerchantId: org.example.project.domain.shared.MerchantId
    )
}
