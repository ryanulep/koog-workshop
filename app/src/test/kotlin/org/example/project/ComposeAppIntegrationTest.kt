package org.example.project

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.example.project.domain.admin.ProductActiveFilter
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.order.OrderStatus
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ComposeAppIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun workspaceSwitchPreservesIndependentFilterState() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.assertProductsWorkspaceVisible()
        harness.products.waitUntilVisibleProducts(scenario.products, scenario.products)

        harness.products.openFilters()
        harness.products.search("moonwell")
        harness.products.selectActiveFilter(ProductActiveFilter.INACTIVE)
        harness.products.waitUntilVisibleProducts(scenario.products, listOf(scenario.inactiveProduct))
        harness.products.closeFilters()
        harness.assertFilterBadge(2)

        harness.switchToOrders()
        harness.assertOrdersWorkspaceVisible()
        harness.assertFilterBadge(0)
        harness.orders.waitUntilVisibleOrders(scenario.orders, scenario.orders)

        harness.orders.openFilters()
        harness.orders.searchOrderId(scenario.newestOrder.orderId.value.toString().take(8))
        harness.orders.selectOrderStatusFilter(OrderStatus.PENDING)
        harness.orders.waitUntilVisibleOrders(scenario.orders, listOf(scenario.newestOrder))
        harness.orders.closeFilters()
        harness.assertFilterBadge(2)

        harness.switchToProducts()
        harness.assertProductsWorkspaceVisible()
        harness.assertFilterBadge(2)
        harness.products.waitUntilVisibleProducts(scenario.products, listOf(scenario.inactiveProduct))
        harness.products.assertDetailShowsProduct(scenario.inactiveProduct, isActive = false)

        harness.switchToOrders()
        harness.assertOrdersWorkspaceVisible()
        harness.assertFilterBadge(2)
        harness.orders.waitUntilVisibleOrders(scenario.orders, listOf(scenario.newestOrder))
        harness.orders.assertDetailShowsOrder(scenario.newestOrder)
        harness.orders.assertStatusMetric(OrderStatus.PENDING)
    }

    @Test
    fun productsLoadAndAutoSelectFirstProductDetail() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.assertProductsWorkspaceVisible()
        harness.products.waitUntilVisibleProducts(scenario.products, scenario.products)

        harness.products.assertDetailShowsProduct(scenario.primaryProduct)
        harness.products.assertDetailContains(
            "Price",
            scenario.primaryProduct.priceText,
            "Stock",
            scenario.primaryProduct.reviewText,
            scenario.primaryProduct.merchantName,
            "Category fields",
            scenario.primaryProduct.categoryFieldLabel,
            scenario.primaryProduct.categoryFieldValue
        )
    }

    @Test
    fun productNameFilterNarrowsListAndSelection() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)

        harness.products.openFilters()
        harness.products.search("moonwell")
        harness.products.waitUntilVisibleProducts(scenario.products, listOf(scenario.inactiveProduct))

        harness.products.assertDetailShowsProduct(scenario.inactiveProduct, isActive = false)
        harness.products.closeFilters()
        harness.assertFilterBadge(1)
    }

    @Test
    fun productStructuredFiltersApplyAndClear() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)

        harness.products.openFilters()
        harness.products.selectActiveFilter(ProductActiveFilter.INACTIVE)
        harness.products.selectCategory(ProductCategory.POTIONS)
        harness.products.selectMerchant(scenario.inactiveProduct.merchantName)
        harness.products.waitUntilVisibleProducts(scenario.products, listOf(scenario.inactiveProduct))
        harness.products.closeFilters()
        harness.assertFilterBadge(3)

        harness.products.openFilters()
        harness.products.selectActiveFilter(ProductActiveFilter.ALL)
        harness.products.selectCategory(null)
        harness.products.selectMerchant("All")
        harness.products.waitUntilVisibleProducts(scenario.products, scenario.products)
        harness.products.closeFilters()
        harness.assertFilterBadge(0)
    }

    @Test
    fun productFiltersShowEmptyStateAndNoDetail() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)

        harness.products.openFilters()
        harness.products.search("void dust")
        harness.products.assertEmptyState()
    }

    @Test
    fun productRefreshReloadsFromDatabase() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.products.waitForRow(scenario.primaryProduct)

        scenario.refreshProductFromDatabase()
        harness.refresh()

        harness.products.assertRowState(scenario.refreshedPrimaryProduct)
        harness.products.assertDetailShowsProduct(scenario.refreshedPrimaryProduct)
        harness.products.assertDetailContains(scenario.refreshedProductDescription)

        val product = scenario.loadProduct()
        assertEquals(scenario.refreshedProductName, product.name)
        assertEquals(scenario.refreshedProductStock, product.stock)
        assertEquals(scenario.refreshedProductDescription, product.description)
    }

    @Test
    fun productDecreaseStockUpdatesUiAndDatabase() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.products.waitForRow(scenario.primaryProduct)

        harness.products.adjustStock(-1)

        harness.products.assertRowState(scenario.primaryProduct, stock = scenario.primaryProduct.stock - 1)
        harness.products.assertDetailContains((scenario.primaryProduct.stock - 1).toString())

        val product = scenario.loadProduct()
        assertEquals(scenario.primaryProduct.stock - 1, product.stock)
    }

    @Test
    fun inactiveProductCanBeReactivated() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)

        harness.products.openFilters()
        harness.products.search("moonwell")
        harness.products.waitUntilVisibleProducts(scenario.products, listOf(scenario.inactiveProduct))
        harness.products.closeFilters()

        harness.products.assertDetailShowsProduct(scenario.inactiveProduct, isActive = false)
        harness.products.toggleActivation(isActive = false)

        harness.products.assertRowState(scenario.inactiveProduct, isActive = true)
        harness.products.assertDetailShowsProduct(scenario.inactiveProduct, isActive = true)

        val product = scenario.loadProduct(scenario.inactiveProduct.id)
        assertTrue(product.isActive)
    }

    @Test
    fun ordersLoadAndAutoSelectNewestOrderDetail() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.switchToOrders()
        harness.assertOrdersWorkspaceVisible()
        harness.orders.waitUntilVisibleOrders(scenario.orders, scenario.orders)

        harness.orders.assertDetailShowsOrder(scenario.newestOrder)
        harness.orders.assertStatusMetric(OrderStatus.PENDING)
        harness.orders.assertSubOrderState(scenario.mutableSubOrder)
        harness.orders.assertDetailContains(
            scenario.primaryProduct.merchantName,
            scenario.mutableSubOrder.merchantName,
            scenario.newestOrder.itemNames.first(),
            scenario.newestOrder.itemNames.last(),
            scenario.newestOrder.historyDescription
        )
    }

    @Test
    fun orderIdFilterNarrowsListAndSelection() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.switchToOrders()

        harness.orders.openFilters()
        harness.orders.searchOrderId(scenario.olderOrder.orderId.value.toString().takeLast(8))
        harness.orders.waitUntilVisibleOrders(scenario.orders, listOf(scenario.olderOrder))

        harness.orders.assertDetailShowsOrder(scenario.olderOrder)
        harness.orders.assertDetailContains(scenario.olderOrder.itemNames.single())
        harness.orders.closeFilters()
        harness.assertFilterBadge(1)
    }

    @Test
    fun orderStructuredFiltersApplyAndClear() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.switchToOrders()

        harness.orders.openFilters()
        harness.orders.selectOrderStatusFilter(OrderStatus.PENDING)
        harness.orders.selectSubOrderStatusFilter(OrderStatus.SHIPPED)
        harness.orders.selectMerchant(scenario.mutableSubOrder.merchantName)
        harness.orders.waitUntilVisibleOrders(scenario.orders, listOf(scenario.newestOrder))
        harness.orders.closeFilters()
        harness.assertFilterBadge(3)

        harness.orders.openFilters()
        harness.orders.selectOrderStatusFilter(null)
        harness.orders.selectSubOrderStatusFilter(null)
        harness.orders.selectMerchant("All")
        harness.orders.waitUntilVisibleOrders(scenario.orders, scenario.orders)
        harness.orders.closeFilters()
        harness.assertFilterBadge(0)
    }

    @Test
    fun orderFiltersShowEmptyStateAndNoDetail() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.switchToOrders()

        harness.orders.openFilters()
        harness.orders.selectOrderStatusFilter(OrderStatus.REFUNDED)
        harness.orders.assertEmptyState()
    }

    @Test
    fun orderRefreshReloadsFromDatabase() {
        val scenario = createAdminAppScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.switchToOrders()
        harness.orders.waitForRow(scenario.newestOrder)

        scenario.refreshOrderFromDatabase()
        harness.refresh()

        harness.orders.assertRowState(scenario.newestOrder, status = scenario.refreshedOrderStatus)
        harness.orders.assertDetailShowsOrder(scenario.newestOrder, status = scenario.refreshedOrderStatus)
        harness.orders.assertStatusMetric(scenario.refreshedOrderStatus)
        harness.orders.assertSubOrderState(scenario.mutableSubOrder, status = scenario.refreshedSubOrderStatus)
        harness.orders.assertDetailContains(scenario.refreshedOrderHistoryDescription)

        val order = scenario.loadOrder()
        val subOrder = scenario.loadSubOrder()
        assertEquals(scenario.refreshedOrderStatus, order.status)
        assertEquals(scenario.refreshedSubOrderStatus, subOrder.status)
    }

    @Test
    fun productStockIncreaseUpdatesUiAndDatabase() {
        val scenario = createProductMutationScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)

        val productRow = hasContentDescription(scenario.rowDescription)
            .and(hasStateDescription(scenario.rowState()))
            .and(hasClickAction())

        rule.waitUntilExists(productRow)
        rule.onNode(productRow).performClick()

        harness.products.adjustStock(5)
        rule.waitUntilExists(
            hasContentDescription(scenario.rowDescription)
                .and(hasStateDescription(scenario.rowState(stock = scenario.initialStock + 5)))
        )
        harness.products.assertDetailContains((scenario.initialStock + 5).toString())
        awaitCondition { scenario.loadProduct().stock == scenario.initialStock + 5 }

        val product = scenario.loadProduct()
        assertEquals(scenario.initialStock + 5, product.stock)
        assertTrue(product.isActive)
    }

    @Test
    fun orderMutationsUpdateUiAndDatabase() {
        val scenario = createOrderMutationScenario()
        val harness = AdminAppHarness(rule)

        harness.launch(scenario.database)
        harness.switchToOrders()

        val orderRow = hasContentDescription(scenario.orderRowDescription)
            .and(hasStateDescription(scenario.orderRowState(OrderStatus.PENDING)))
            .and(hasClickAction())

        rule.waitUntilExists(orderRow)
        rule.onNode(orderRow).performClick()

        harness.orders.updateOrderStatus(OrderStatus.DELIVERED)
        rule.waitUntilExists(
            hasContentDescription(scenario.orderRowDescription)
                .and(hasStateDescription(scenario.orderRowState(OrderStatus.DELIVERED)))
        )

        harness.orders.updateSubOrderStatus(
            AdminSubOrderFixture(
                id = scenario.subOrderId,
                merchantName = scenario.merchantName,
                initialStatus = OrderStatus.PENDING
            ),
            OrderStatus.SHIPPED
        )
        rule.waitUntilExists(
            hasContentDescription(scenario.subOrderDescription)
                .and(hasStateDescription("Shipped"))
        )

        harness.orders.assertStatusMetric(OrderStatus.DELIVERED)
        harness.orders.assertDetailContains("Order updated", "Sub-order updated")

        val order = scenario.loadOrder()
        val subOrder = scenario.loadSubOrder()
        assertEquals(OrderStatus.DELIVERED, order.status)
        assertEquals(OrderStatus.SHIPPED, subOrder.status)
    }

    private fun awaitCondition(
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) {
                "Condition not met within ${timeoutMillis}ms"
            }
            Thread.sleep(10)
        }
    }
}
