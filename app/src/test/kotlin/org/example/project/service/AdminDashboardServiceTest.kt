package org.example.project.domain.admin

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.character.Characters
import org.example.project.domain.currency.Currencies
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.order.OrderItems
import org.example.project.domain.order.Orders
import org.example.project.domain.catalog.Products
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.order.SubOrders
import org.example.project.domain.character.Transactions
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.character.TransactionType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class AdminDashboardServiceTest {

    @Test
    fun `loadOrderHistory returns all orders in newest-first order`() = runBlocking {
        val database = createDatabase()
        seedOrderHistory(database)
        val service = AdminDashboardService(database)

        val orders = service.loadOrderHistory()

        assertEquals(2, orders.size)
        assertEquals(listOf(Instant.fromEpochMilliseconds(2_000), Instant.fromEpochMilliseconds(1_000)), orders.map { it.createdAt })
        assertEquals(listOf(OrderStatus.DELIVERED, OrderStatus.PENDING), orders.map { it.status })
        assertTrue(orders.all { it.characterName == "Aldric" })
        assertTrue(orders.all { it.totalCurrencyCode == "GOLD" })
    }

    @Test
    fun `loadOrderDetailsOrNull returns the order graph and history timeline`() = runBlocking {
        val database = createDatabase()
        val orderId = seedOrderDetails(database)
        val service = AdminDashboardService(database)

        val detail = service.loadOrderDetailsOrNull(orderId)

        assertNotNull(detail)
        assertEquals(orderId, detail.order.id)
        assertEquals("Aldric", detail.characterName)
        assertEquals("GOLD", detail.currencyCode)
        assertEquals(1, detail.subOrders.size)

        val subOrder = detail.subOrders.single()
        assertEquals("Blackforge Armory", subOrder.merchantName)
        assertEquals("Courier Raven", subOrder.shippingMethodName)
        assertEquals(1, subOrder.items.size)

        val item = subOrder.items.single()
        assertEquals("Traveler's Kit", item.productName)
        assertEquals("MISCELLANEOUS", item.productCategory)
        assertEquals(2, item.item.quantity)
        assertEquals(500L, item.subtotal)

        val historyTitles = detail.history.map { it.title }
        assertTrue(historyTitles.contains("Order created"))
        assertTrue(historyTitles.contains("Purchase recorded"))
        assertTrue(historyTitles.contains("Refund recorded"))
        assertTrue(historyTitles.contains("Order updated"))
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("admin_dashboard_", ".db").apply {
            deleteOnExit()
        }
        return Database.connect("jdbc:sqlite:${databaseFile.absolutePath}").createTables()
    }

    private fun seedOrderHistory(database: Database) {
        transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }

            val characterId = Characters.insertAndGetId {
                it[name] = "Aldric"
            }

            Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.PENDING.name
                it[totalPrice] = 1_000
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(1_000)
            }
            Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.DELIVERED.name
                it[totalPrice] = 2_000
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(2_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }
        }
    }

    private fun seedOrderDetails(database: Database): org.example.project.domain.shared.OrderId {
        return transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }

            val characterId = Characters.insertAndGetId {
                it[name] = "Aldric"
            }

            val merchantId = Merchants.insertAndGetId {
                it[name] = "Blackforge Armory"
                it[description] = "Battle-tested steel and frontier gear."
            }

            val shippingMethodId = ShippingMethods.insertAndGetId {
                it[name] = "Courier Raven"
                it[baseCost] = 25
                it[currency] = goldId
                it[estimatedDays] = 3
            }

            val productId = Products.insertAndGetId {
                it[name] = "Traveler's Kit"
                it[description] = "A compact set of survival gear."
                it[category] = ProductCategory.MISCELLANEOUS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 250
                it[currency] = goldId
                it[merchant] = merchantId
                it[stock] = 12
            }

            val orderId = Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.REFUNDED.name
                it[totalPrice] = 525
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
            }

            val subOrderId = SubOrders.insertAndGetId {
                it[order] = orderId
                it[merchant] = merchantId
                it[status] = OrderStatus.REFUNDED.name
                it[shippingMethod] = shippingMethodId
                it[shippingCost] = 25
                it[merchantTotalPrice] = 525
                it[SubOrders.createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[SubOrders.updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }

            OrderItems.insertAndGetId {
                it[subOrder] = subOrderId
                it[product] = productId
                it[quantity] = 2
                it[snapshottedPrice] = 250
                it[snapshottedCurrency] = goldId
                it[OrderItems.createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[OrderItems.updatedAt] = Instant.fromEpochMilliseconds(1_000)
            }

            Transactions.insertAndGetId {
                it[character] = characterId
                it[currency] = goldId
                it[amount] = -525
                it[type] = TransactionType.PURCHASE.name
                it[referenceId] = orderId.value
                it[referenceType] = "ORDER"
                it[description] = "Purchase for order ${orderId.value}"
                it[Transactions.createdAt] = Instant.fromEpochMilliseconds(1_500)
                it[Transactions.updatedAt] = Instant.fromEpochMilliseconds(1_500)
            }
            Transactions.insertAndGetId {
                it[character] = characterId
                it[currency] = goldId
                it[amount] = 525
                it[type] = TransactionType.REFUND.name
                it[referenceId] = orderId.value
                it[referenceType] = "ORDER"
                it[description] = "Refund completed for order ${orderId.value}"
                it[Transactions.createdAt] = Instant.fromEpochMilliseconds(2_500)
                it[Transactions.updatedAt] = Instant.fromEpochMilliseconds(2_500)
            }

            org.example.project.domain.shared.OrderId(orderId.value)
        }
    }
}
