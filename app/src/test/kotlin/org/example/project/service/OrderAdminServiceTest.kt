package org.example.project.domain.admin.orders

import kotlinx.coroutines.runBlocking
import org.example.project.admin.orders.operations.AdminOrderService
import org.example.project.admin.orders.operations.OrderFilter
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.character.Characters
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.OrderItems
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.Orders
import org.example.project.domain.order.SubOrders
import org.example.project.domain.shipping.ShippingMethods
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class OrderServiceTest {

    @Test
    fun `loadOrders applies merchant and status filters`() = runBlocking {
        val database = createDatabase()
        val fixture = seedOrders(database)
        val service = AdminOrderService(database)
        val filteredOrders = service.loadOrders(
            OrderFilter(
                orderStatus = OrderStatus.PENDING,
                subOrderStatus = OrderStatus.SHIPPED,
                merchantId = fixture.moonwellMerchantId
            )
        )

        assertEquals(listOf(fixture.pendingOrderId), filteredOrders.map { it.orderId })
        assertEquals(2, filteredOrders.single().merchantCount)
    }

    @Test
    fun `loadOrderDetailOrNull returns hierarchy and respects status updates`() = runBlocking {
        val database = createDatabase()
        val fixture = seedOrders(database)
        val service = AdminOrderService(database)

        val beforeUpdate = service.loadOrderDetailOrNull(fixture.pendingOrderId)

        assertNotNull(beforeUpdate)
        assertEquals(2, beforeUpdate.subOrders.size)
        assertTrue(beforeUpdate.subOrders.any { it.merchantName == "Moonwell Remedies" })
        assertEquals(
            listOf("Bronze Blade", "Moonwell Draught"),
            beforeUpdate.subOrders.flatMap { detail -> detail.items.map { item -> item.productName } }.sorted()
        )

        assertTrue(service.updateSubOrderStatus(fixture.shippedSubOrderId, OrderStatus.DELIVERED))

        val afterUpdate = service.loadOrderDetailOrNull(fixture.pendingOrderId)

        assertNotNull(afterUpdate)
        assertEquals(
            OrderStatus.DELIVERED,
            afterUpdate.subOrders.single { detail -> detail.subOrder.id == fixture.shippedSubOrderId }.subOrder.status
        )
    }

    @Test
    fun `updateOrderStatus updates the top-level order`() = runBlocking {
        val database = createDatabase()
        val fixture = seedOrders(database)
        val service = AdminOrderService(database)

        assertTrue(service.updateOrderStatus(fixture.pendingOrderId, OrderStatus.CANCELLED))

        val afterUpdate = service.loadOrderDetailOrNull(fixture.pendingOrderId)

        assertNotNull(afterUpdate)
        assertEquals(OrderStatus.CANCELLED, afterUpdate.order.status)
        assertTrue(afterUpdate.order.updatedAt > afterUpdate.order.createdAt)
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("order_admin_", ".db").apply {
            deleteOnExit()
        }
        return connectSqlite(databaseFile).createTables()
    }

    private fun seedOrders(database: Database): OrderFixture =
        transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }

            val aldricId = Characters.insertAndGetId {
                it[name] = "Aldric"
            }
            val brannaId = Characters.insertAndGetId {
                it[name] = "Branna"
            }

            val blackforgeId = Merchants.insertAndGetId {
                it[name] = "Blackforge Armory"
            }
            val moonwellId = Merchants.insertAndGetId {
                it[name] = "Moonwell Remedies"
            }

            val ravenShippingId = ShippingMethods.insertAndGetId {
                it[name] = "Courier Raven"
                it[baseCost] = 20
                it[currency] = goldId
                it[estimatedDays] = 2
            }
            val portalShippingId = ShippingMethods.insertAndGetId {
                it[name] = "Portal Relay"
                it[baseCost] = 35
                it[currency] = goldId
                it[estimatedDays] = 1
            }

            val bronzeBladeId = Products.insertAndGetId {
                it[name] = "Bronze Blade"
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.UNCOMMON.name
                it[price] = 320
                it[currency] = goldId
                it[merchant] = blackforgeId
                it[stock] = 12
            }
            val moonwellDraughtId = Products.insertAndGetId {
                it[name] = "Moonwell Draught"
                it[category] = ProductCategory.POTIONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 90
                it[currency] = goldId
                it[merchant] = moonwellId
                it[stock] = 8
            }

            val pendingOrderId = Orders.insertAndGetId {
                it[character] = aldricId
                it[status] = OrderStatus.PENDING.name
                it[totalPrice] = 445
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(3_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
            }
            val deliveredOrderId = Orders.insertAndGetId {
                it[character] = brannaId
                it[status] = OrderStatus.DELIVERED.name
                it[totalPrice] = 320
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }

            val confirmedSubOrderId = SubOrders.insertAndGetId {
                it[order] = pendingOrderId
                it[merchant] = blackforgeId
                it[status] = OrderStatus.CONFIRMED.name
                it[shippingMethod] = ravenShippingId
                it[shippingCost] = 20
                it[merchantTotalPrice] = 340
                it[createdAt] = Instant.fromEpochMilliseconds(3_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_200)
            }
            val shippedSubOrderId = SubOrders.insertAndGetId {
                it[order] = pendingOrderId
                it[merchant] = moonwellId
                it[status] = OrderStatus.SHIPPED.name
                it[shippingMethod] = portalShippingId
                it[shippingCost] = 35
                it[merchantTotalPrice] = 105
                it[createdAt] = Instant.fromEpochMilliseconds(3_100)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_400)
            }
            val deliveredSubOrderId = SubOrders.insertAndGetId {
                it[order] = deliveredOrderId
                it[merchant] = blackforgeId
                it[status] = OrderStatus.DELIVERED.name
                it[shippingMethod] = ravenShippingId
                it[shippingCost] = 20
                it[merchantTotalPrice] = 340
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }

            OrderItems.insertAndGetId {
                it[subOrder] = confirmedSubOrderId
                it[product] = bronzeBladeId
                it[quantity] = 1
                it[snapshottedPrice] = 320
                it[snapshottedCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(3_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
            }
            OrderItems.insertAndGetId {
                it[subOrder] = shippedSubOrderId
                it[product] = moonwellDraughtId
                it[quantity] = 1
                it[snapshottedPrice] = 90
                it[snapshottedCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(3_100)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_100)
            }
            OrderItems.insertAndGetId {
                it[subOrder] = deliveredSubOrderId
                it[product] = bronzeBladeId
                it[quantity] = 1
                it[snapshottedPrice] = 320
                it[snapshottedCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(1_000)
            }

            OrderFixture(
                pendingOrderId = org.example.project.domain.shared.OrderId(pendingOrderId.value),
                shippedSubOrderId = org.example.project.domain.shared.SubOrderId(shippedSubOrderId.value),
                moonwellMerchantId = org.example.project.domain.shared.MerchantId(moonwellId.value)
            )
        }

    private data class OrderFixture(
        val pendingOrderId: org.example.project.domain.shared.OrderId,
        val shippedSubOrderId: org.example.project.domain.shared.SubOrderId,
        val moonwellMerchantId: org.example.project.domain.shared.MerchantId
    )
}
