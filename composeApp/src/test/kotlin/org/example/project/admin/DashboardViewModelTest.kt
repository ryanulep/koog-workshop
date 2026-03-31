package org.example.project.admin

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.db.tables.Characters
import org.example.project.db.tables.Currencies
import org.example.project.db.tables.Merchants
import org.example.project.db.tables.Orders
import org.example.project.db.tables.Products
import org.example.project.db.tables.ShippingMethods
import org.example.project.domain.enums.OrderStatus
import org.example.project.service.AdminDashboardService
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class DashboardViewModelTest {

    @Test
    fun `loadDashboard maps an empty database to Empty`() = runBlocking {
        val database = createDatabase()
        val viewModel = DashboardViewModel(AdminDashboardService(database))

        viewModel.loadDashboard()

        assertEquals(DashboardUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `loadDashboard maps dashboard data to Ready`() = runBlocking {
        val database = createDatabase()
        seedDashboardData(database)
        val viewModel = DashboardViewModel(AdminDashboardService(database))

        viewModel.loadDashboard()

        val state = viewModel.uiState.value
        assertIs<DashboardUiState.Ready>(state)
        assertEquals(4L, state.summary.totalProducts)
        assertEquals(2L, state.summary.totalMerchants)
        assertEquals(2L, state.summary.totalOrders)
        assertEquals(1L, state.summary.totalCharacters)
        assertEquals(1L, state.summary.totalShippingMethods)
        assertEquals(listOf(1, 3, 5), state.lowStockProducts.map { it.stock })
        assertEquals(
            listOf("Mana Potion", "Healing Potion", "Steel Shield"),
            state.lowStockProducts.map { it.productName }
        )
        assertEquals(
            listOf(Instant.fromEpochMilliseconds(2_000), Instant.fromEpochMilliseconds(1_000)),
            state.recentOrders.map { it.createdAt }
        )
        assertEquals(
            listOf(OrderStatus.DELIVERED, OrderStatus.PENDING),
            state.recentOrders.map { it.status }
        )
        assertTrue(state.recentOrders.all { it.totalCurrencyCode == "GOLD" })
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("dashboard_viewmodel_", ".db").apply {
            deleteOnExit()
        }
        return Database.connect("jdbc:sqlite:${databaseFile.absolutePath}").createTables()
    }

    private fun seedDashboardData(database: Database) {
        transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }

            val blacksmithId = Merchants.insertAndGetId {
                it[name] = "Blacksmith"
            }
            val alchemistId = Merchants.insertAndGetId {
                it[name] = "Alchemist"
            }
            val characterId = Characters.insertAndGetId {
                it[name] = "Aldric"
            }

            ShippingMethods.insertAndGetId {
                it[name] = "Courier Raven"
                it[baseCost] = 50
                it[currency] = goldId
                it[estimatedDays] = 3
            }

            Products.insertAndGetId {
                it[name] = "Mana Potion"
                it[category] = "POTIONS"
                it[rarity] = "COMMON"
                it[price] = 70
                it[currency] = goldId
                it[merchant] = alchemistId
                it[stock] = 1
            }
            Products.insertAndGetId {
                it[name] = "Healing Potion"
                it[category] = "POTIONS"
                it[rarity] = "COMMON"
                it[price] = 80
                it[currency] = goldId
                it[merchant] = alchemistId
                it[stock] = 3
            }
            Products.insertAndGetId {
                it[name] = "Steel Shield"
                it[category] = "ARMOR"
                it[rarity] = "COMMON"
                it[price] = 250
                it[currency] = goldId
                it[merchant] = blacksmithId
                it[stock] = 5
            }
            Products.insertAndGetId {
                it[name] = "Plate Armour"
                it[category] = "ARMOR"
                it[rarity] = "RARE"
                it[price] = 900
                it[currency] = goldId
                it[merchant] = blacksmithId
                it[stock] = 12
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
}
