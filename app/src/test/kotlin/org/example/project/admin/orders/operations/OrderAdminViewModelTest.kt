package org.example.project.admin.orders.operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, kotlin.uuid.ExperimentalUuidApi::class)
class OrderAdminViewModelTest {

    @Test
    fun `load selects the newest order detail`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedOrders(database)
            val viewModel = OrderAdminViewModel(AdminOrderService(database))

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedOrderId == fixture.pendingOrderId &&
                        viewModel.uiState.value.selectedOrder != null
            }

            val state = viewModel.uiState.value
            assertEquals(fixture.pendingOrderId, state.selectedOrderId)
            assertNotNull(state.selectedOrder)
            assertEquals(2, state.selectedOrder.subOrders.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `status updates reload selected order`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedOrders(database)
            val viewModel = OrderAdminViewModel(AdminOrderService(database))

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedOrderId == fixture.pendingOrderId &&
                        viewModel.uiState.value.selectedOrder != null
            }

            viewModel.updateSubOrderStatus(fixture.shippedSubOrderId, OrderStatus.DELIVERED)
            awaitCondition {
                viewModel.uiState.value.selectedOrder
                    ?.subOrders
                    ?.singleOrNull { detail -> detail.subOrder.id == fixture.shippedSubOrderId }
                    ?.subOrder
                    ?.status == OrderStatus.DELIVERED
            }

            val state = viewModel.uiState.value
            val updatedSubOrder = state.selectedOrder
                ?.subOrders
                ?.single { detail -> detail.subOrder.id == fixture.shippedSubOrderId }

            assertNotNull(updatedSubOrder)
            assertEquals(OrderStatus.DELIVERED, updatedSubOrder.subOrder.status)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `order status updates reload selected order`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedOrders(database)
            val viewModel = OrderAdminViewModel(AdminOrderService(database))

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedOrderId == fixture.pendingOrderId &&
                        viewModel.uiState.value.selectedOrder != null
            }

            viewModel.updateOrderStatus(fixture.pendingOrderId, OrderStatus.CANCELLED)
            awaitCondition {
                viewModel.uiState.value.selectedOrder
                    ?.order
                    ?.status == OrderStatus.CANCELLED
            }

            val state = viewModel.uiState.value
            assertNotNull(state.selectedOrder)
            assertEquals(OrderStatus.CANCELLED, state.selectedOrder.order.status)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("order_vm_", ".db").apply {
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
            val draughtId = Products.insertAndGetId {
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
                it[product] = draughtId
                it[quantity] = 1
                it[snapshottedPrice] = 90
                it[snapshottedCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(3_100)
                it[updatedAt] = Instant.fromEpochMilliseconds(3_100)
            }

            OrderFixture(
                pendingOrderId = org.example.project.domain.shared.OrderId(pendingOrderId.value),
                shippedSubOrderId = org.example.project.domain.shared.SubOrderId(shippedSubOrderId.value)
            )
        }

    private fun awaitCondition(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) {
                "Condition not met within ${timeoutMillis}ms"
            }
            Thread.sleep(10)
        }
    }

    private data class OrderFixture(
        val pendingOrderId: org.example.project.domain.shared.OrderId,
        val shippedSubOrderId: org.example.project.domain.shared.SubOrderId
    )
}
