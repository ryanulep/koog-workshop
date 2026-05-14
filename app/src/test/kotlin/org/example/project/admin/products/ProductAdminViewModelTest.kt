package org.example.project.admin.products

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Rarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProductAdminViewModelTest {

    private class MockAdminProductService : AdminProductService(io.ktor.client.HttpClient()) {
        var products = mutableListOf<ProductListItem>()
        var productDetails = mutableMapOf<ProductId, ProductDetail>()
        var merchantOptions = mutableListOf<ProductMerchantOption>()

        override suspend fun loadMerchantOptions(): List<ProductMerchantOption> = merchantOptions
        override suspend fun loadProducts(filter: ProductFilter): List<ProductListItem> = products
        override suspend fun loadProductDetailOrNull(productId: ProductId): ProductDetail? = productDetails[productId]
        override suspend fun adjustStock(productId: ProductId, quantityChange: Int): Boolean = true
        override suspend fun setProductActive(productId: ProductId, isActive: Boolean): Boolean {
            val p = products.find { it.id == productId }
            if (p != null) {
                products[products.indexOf(p)] = p.copy(isActive = isActive)
            }
            val d = productDetails[productId]
            if (d != null) {
                productDetails[productId] = d.copy(isActive = isActive)
            }
            return true
        }
    }

    @Test
    fun `load selects the first product detail`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val service = MockAdminProductService()
            val bronzeBladeId = ProductId(Uuid.random())
            val moonwellDraughtId = ProductId(Uuid.random())

            service.products.addAll(listOf(
                ProductListItem(bronzeBladeId, "Bronze Blade", ProductCategory.WEAPONS, "Blackforge Armory", 320, "GOLD", 12, true, ProductReviewSummary()),
                ProductListItem(moonwellDraughtId, "Moonwell Draught", ProductCategory.POTIONS, "Moonwell Remedies", 90, "GOLD", 6, false, ProductReviewSummary())
            ))
            service.productDetails[bronzeBladeId] = ProductDetail(
                id = bronzeBladeId,
                name = "Bronze Blade",
                description = "A basic blade",
                category = ProductCategory.WEAPONS,
                rarity = Rarity.UNCOMMON,
                price = 320,
                currencyCode = "GOLD",
                currencySymbol = "G",
                merchantId = MerchantId(Uuid.random()),
                merchantName = "Blackforge Armory",
                stock = 12,
                isActive = true,
                imageUrl = null,
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
                reviewSummary = ProductReviewSummary(),
                categoryAttributes = emptyList()
            )

            val viewModel = ProductAdminViewModel(service)

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.products.isNotEmpty() &&
                        viewModel.uiState.value.selectedProduct != null
            }

            val state = viewModel.uiState.value
            assertEquals(listOf(bronzeBladeId, moonwellDraughtId), state.products.map { it.id })
            assertEquals(bronzeBladeId, state.selectedProductId)
            assertNotNull(state.selectedProduct)
            assertEquals("Blackforge Armory", state.selectedProduct?.merchantName)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `active filter refreshes products and selection`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val service = MockAdminProductService()
            val bronzeBladeId = ProductId(Uuid.random())
            val moonwellDraughtId = ProductId(Uuid.random())

            val allProducts = listOf(
                ProductListItem(bronzeBladeId, "Bronze Blade", ProductCategory.WEAPONS, "Blackforge Armory", 320, "GOLD", 12, true, ProductReviewSummary()),
                ProductListItem(moonwellDraughtId, "Moonwell Draught", ProductCategory.POTIONS, "Moonwell Remedies", 90, "GOLD", 6, false, ProductReviewSummary())
            )
            service.products.addAll(allProducts)
            service.productDetails[bronzeBladeId] = ProductDetail(
                id = bronzeBladeId,
                name = "Bronze Blade",
                description = "A basic blade",
                category = ProductCategory.WEAPONS,
                rarity = Rarity.UNCOMMON,
                price = 320,
                currencyCode = "GOLD",
                currencySymbol = "G",
                merchantId = MerchantId(Uuid.random()),
                merchantName = "Blackforge Armory",
                stock = 12,
                isActive = true,
                imageUrl = null,
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
                reviewSummary = ProductReviewSummary(),
                categoryAttributes = emptyList()
            )
            service.productDetails[moonwellDraughtId] = ProductDetail(
                id = moonwellDraughtId,
                name = "Moonwell Draught",
                description = "Restore stamina",
                category = ProductCategory.POTIONS,
                rarity = Rarity.COMMON,
                price = 90,
                currencyCode = "GOLD",
                currencySymbol = "G",
                merchantId = MerchantId(Uuid.random()),
                merchantName = "Moonwell Remedies",
                stock = 6,
                isActive = false,
                imageUrl = null,
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
                reviewSummary = ProductReviewSummary(),
                categoryAttributes = emptyList()
            )

            val viewModel = ProductAdminViewModel(service)
            // Initial refresh to populate
            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.products.isNotEmpty() &&
                        viewModel.uiState.value.selectedProduct != null
            }

            // Mock the behavior of filtering during reload in a way that doesn't require overriding the ViewModel
            service.products.clear()
            service.products.addAll(allProducts.filter { !it.isActive })

            viewModel.updateActiveFilter(ProductActiveFilter.INACTIVE)
            awaitCondition {
                viewModel.uiState.value.products.map { it.id } == listOf(moonwellDraughtId) &&
                        viewModel.uiState.value.selectedProductId == moonwellDraughtId
            }

            val state = viewModel.uiState.value
            assertEquals(listOf(moonwellDraughtId), state.products.map { it.id })
            assertEquals(moonwellDraughtId, state.selectedProductId)
            assertEquals("Moonwell Draught", state.selectedProduct?.name)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `setSelectedProductActive updates selected product and persisted state`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val service = MockAdminProductService()
            val bronzeBladeId = ProductId(Uuid.random())

            service.products.add(ProductListItem(bronzeBladeId, "Bronze Blade", ProductCategory.WEAPONS, "Blackforge Armory", 320, "GOLD", 12, true, ProductReviewSummary()))
            service.productDetails[bronzeBladeId] = ProductDetail(
                id = bronzeBladeId,
                name = "Bronze Blade",
                description = "A basic blade",
                category = ProductCategory.WEAPONS,
                rarity = Rarity.UNCOMMON,
                price = 320,
                currencyCode = "GOLD",
                currencySymbol = "G",
                merchantId = MerchantId(Uuid.random()),
                merchantName = "Blackforge Armory",
                stock = 12,
                isActive = true,
                imageUrl = null,
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
                reviewSummary = ProductReviewSummary(),
                categoryAttributes = emptyList()
            )

            val viewModel = ProductAdminViewModel(service)

            viewModel.refresh()
            awaitCondition {
                viewModel.uiState.value.selectedProductId == bronzeBladeId &&
                        viewModel.uiState.value.selectedProduct != null
            }

            viewModel.setSelectedProductActive(false)
            awaitCondition {
                viewModel.uiState.value.selectedProduct?.isActive == false &&
                        viewModel.uiState.value.products
                            .firstOrNull { product -> product.id == bronzeBladeId }
                            ?.isActive == false
            }

            val state = viewModel.uiState.value
            assertEquals(false, state.selectedProduct?.isActive)
            assertEquals(
                false,
                state.products.firstOrNull { product -> product.id == bronzeBladeId }?.isActive
            )
        } finally {
            Dispatchers.resetMain()
        }
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
}
