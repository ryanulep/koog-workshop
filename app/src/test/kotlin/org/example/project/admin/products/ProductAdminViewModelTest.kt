package org.example.project.admin.products

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Potions
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.catalog.Weapons
import org.example.project.domain.currency.Currencies
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class, kotlin.uuid.ExperimentalUuidApi::class)
class ProductAdminViewModelTest {

    @Test
    fun `load selects the first product detail`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedProducts(database)
            val viewModel = ProductAdminViewModel(AdminProductService(database))

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.products.isNotEmpty() &&
                        viewModel.uiState.value.selectedProduct != null
            }

            val state = viewModel.uiState.value
            val selectedProduct = state.selectedProduct
            assertEquals(listOf(fixture.bronzeBladeId, fixture.moonwellDraughtId), state.products.map { it.id })
            assertEquals(fixture.bronzeBladeId, state.selectedProductId)
            assertNotNull(selectedProduct)
            assertEquals("Blackforge Armory", selectedProduct.merchantName)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `active filter refreshes products and selection`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedProducts(database)
            val viewModel = ProductAdminViewModel(AdminProductService(database))

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.products.isNotEmpty() &&
                        viewModel.uiState.value.selectedProduct != null
            }

            viewModel.updateActiveFilter(ProductActiveFilter.INACTIVE)
            awaitCondition {
                viewModel.uiState.value.products.map { it.id } == listOf(fixture.moonwellDraughtId) &&
                        viewModel.uiState.value.selectedProductId == fixture.moonwellDraughtId
            }

            val state = viewModel.uiState.value
            assertEquals(listOf(fixture.moonwellDraughtId), state.products.map { it.id })
            assertEquals(fixture.moonwellDraughtId, state.selectedProductId)
            assertEquals("Moonwell Draught", state.selectedProduct?.name)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `setSelectedProductActive updates selected product and persisted state`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val database = createDatabase()
            val fixture = seedProducts(database)
            val service = AdminProductService(database)
            val viewModel = ProductAdminViewModel(service)

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedProductId == fixture.bronzeBladeId &&
                        viewModel.uiState.value.selectedProduct != null
            }

            viewModel.setSelectedProductActive(false)
            awaitCondition {
                viewModel.uiState.value.selectedProduct?.isActive == false &&
                        viewModel.uiState.value.products
                            .firstOrNull { product -> product.id == fixture.bronzeBladeId }
                            ?.isActive == false
            }

            val state = viewModel.uiState.value
            assertEquals(false, state.selectedProduct?.isActive)
            assertEquals(
                false,
                state.products.firstOrNull { product -> product.id == fixture.bronzeBladeId }?.isActive
            )
            assertEquals(
                false,
                runBlocking {
                    service.loadProductDetailOrNull(fixture.bronzeBladeId)?.isActive
                }
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("product_vm_", ".db").apply {
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

            ProductFixture(
                bronzeBladeId = org.example.project.domain.shared.ProductId(bronzeBladeId.value),
                moonwellDraughtId = org.example.project.domain.shared.ProductId(moonwellDraughtId.value)
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

    private data class ProductFixture(
        val bronzeBladeId: org.example.project.domain.shared.ProductId,
        val moonwellDraughtId: org.example.project.domain.shared.ProductId
    )
}
