package org.example.project.admin.merchants

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.admin.MerchantAdminService
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

@OptIn(ExperimentalCoroutinesApi::class, kotlin.uuid.ExperimentalUuidApi::class)
class MerchantAdminViewModelTest {

    @Test
    fun `load selects the first merchant detail and shipping draft`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedMerchants(database)
            val viewModel = MerchantAdminViewModel(MerchantAdminService(database))

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.merchants.isNotEmpty() &&
                        viewModel.uiState.value.selectedMerchant != null
            }

            val state = viewModel.uiState.value
            assertEquals(
                listOf(fixture.blackforgeMerchantId, fixture.moonwellMerchantId),
                state.merchants.map { it.id })
            assertEquals(fixture.blackforgeMerchantId, state.selectedMerchantId)
            assertEquals("Blackforge Armory", state.selectedMerchant?.merchant?.name)
            assertEquals(setOf(fixture.ravenShippingId), state.selectedShippingMethodIds.toSet())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `selectMerchant and active toggles update ui state and persistence`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedMerchants(database)
            val service = MerchantAdminService(database)
            val viewModel = MerchantAdminViewModel(service)

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedMerchantId == fixture.blackforgeMerchantId
            }

            viewModel.selectMerchant(fixture.moonwellMerchantId)
            awaitCondition {
                viewModel.uiState.value.selectedMerchantId == fixture.moonwellMerchantId &&
                    viewModel.uiState.value.selectedMerchant?.merchant?.name == "Moonwell Remedies"
            }

            viewModel.setSelectedMerchantActive(false)
            awaitCondition {
                viewModel.uiState.value.selectedMerchant?.merchant?.isActive == false
            }

            viewModel.setShippingMethodActive(fixture.portalShippingId, false)
            awaitCondition {
                viewModel.uiState.value.selectedMerchant
                    ?.availableShippingMethods
                    ?.single { shippingMethod -> shippingMethod.id == fixture.portalShippingId }
                    ?.isActive == false
            }

            assertFalse(runBlocking { service.loadMerchantDetailOrNull(fixture.moonwellMerchantId)?.merchant?.isActive ?: true })
            assertFalse(
                runBlocking {
                    service.loadMerchantDetailOrNull(fixture.moonwellMerchantId)
                        ?.availableShippingMethods
                        ?.single { shippingMethod -> shippingMethod.id == fixture.portalShippingId }
                        ?.isActive
                        ?: true
                }
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `saveShippingAssignments replaces assignments and refreshes detail`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedMerchants(database)
            val service = MerchantAdminService(database)
            val viewModel = MerchantAdminViewModel(service)

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedMerchantId == fixture.blackforgeMerchantId
            }

            viewModel.updateShippingAssignmentSelection(fixture.ravenShippingId, false)
            viewModel.updateShippingAssignmentSelection(fixture.portalShippingId, true)
            assertTrue(viewModel.uiState.value.hasPendingShippingAssignments)

            viewModel.saveShippingAssignments()
            awaitCondition {
                viewModel.uiState.value.selectedMerchant
                    ?.assignedShippingMethods
                    ?.map { shippingMethod -> shippingMethod.id }
                    ?.toSet() == setOf(fixture.portalShippingId)
            }

            assertEquals(setOf(fixture.portalShippingId), viewModel.uiState.value.selectedShippingMethodIds.toSet())
            assertEquals(
                setOf(fixture.portalShippingId),
                runBlocking {
                    service.loadMerchantDetailOrNull(fixture.blackforgeMerchantId)
                        ?.assignedShippingMethods
                        ?.map { shippingMethod -> shippingMethod.id }
                        ?.toSet()
                }
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `saveShippingAssignments surfaces failure state`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedMerchants(database)
            val viewModel = MerchantAdminViewModel(MerchantAdminService(database))

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedMerchantId == fixture.blackforgeMerchantId
            }

            viewModel.updateShippingAssignmentSelection(fixture.skyshipShippingId, true)

            transaction(database) {
                assertTrue(ShippingRepository().deleteShippingMethod(fixture.skyshipShippingId))
            }

            viewModel.saveShippingAssignments()
            awaitCondition {
                viewModel.uiState.value.errorMessage != null
            }

            assertNotNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("merchant_vm_", ".db").apply {
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
            val skyshipId = ShippingMethods.insertAndGetId {
                it[name] = "Skyship Express"
                it[description] = "Unreferenced emergency route."
                it[baseCost] = 55
                it[currency] = goldId
                it[estimatedDays] = 1
                it[createdAt] = Instant.fromEpochMilliseconds(5_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(5_000)
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
                portalShippingId = ShippingMethodId(portalId.value),
                skyshipShippingId = ShippingMethodId(skyshipId.value)
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

    private data class MerchantFixture(
        val blackforgeMerchantId: MerchantId,
        val moonwellMerchantId: MerchantId,
        val ravenShippingId: ShippingMethodId,
        val portalShippingId: ShippingMethodId,
        val skyshipShippingId: ShippingMethodId
    )
}
