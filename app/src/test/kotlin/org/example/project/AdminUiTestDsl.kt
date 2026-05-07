@file:OptIn(ExperimentalTestApi::class)

package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.example.project.admin.app.AdminRoute
import org.example.project.admin.merchants.AdminMerchantService
import org.example.project.admin.orders.operations.AdminOrderService
import org.example.project.admin.products.AdminProductService
import org.example.project.admin.products.ProductActiveFilter
import org.example.project.admin.shared.ui.AdminAccessibility
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.order.OrderStatus
import org.jetbrains.exposed.v1.jdbc.Database

@Composable
internal fun AdminApp(database: Database) {
    val services = Dependencies.Services(
        productService = AdminProductService(database),
        merchantService = AdminMerchantService(database),
        orderService = AdminOrderService(database)
    )
    AdminRoute(services = services)
}

internal class AdminAppHarness(
    private val rule: ComposeContentTestRule
) {
    val products: ProductsRobot = ProductsRobot(rule)
    val orders: OrdersRobot = OrdersRobot(rule)

    fun launch(database: Database) {
        rule.setContent {
            AdminApp(database = database)
        }
        rule.waitUntilExists(hasContentDescription(AdminAccessibility.ProductListPanel))
    }

    fun switchToProducts() {
        rule.onNode(workspaceTabMatcher("Products")).performClick()
        rule.waitUntilExists(hasContentDescription(AdminAccessibility.ProductListPanel))
    }

    fun switchToOrders() {
        rule.onNode(workspaceTabMatcher("Orders")).performClick()
        rule.waitUntilExists(hasContentDescription(AdminAccessibility.OrderListPanel))
    }

    fun refresh() {
        rule.onNode(hasText("Refresh").and(hasClickAction())).performClick()
    }

    fun assertProductsWorkspaceVisible() {
        rule.onNodeWithContentDescription(AdminAccessibility.ProductListPanel).assertExists()
        rule.onNodeWithContentDescription(AdminAccessibility.OrderListPanel).assertDoesNotExist()
    }

    fun assertOrdersWorkspaceVisible() {
        rule.onNodeWithContentDescription(AdminAccessibility.OrderListPanel).assertExists()
        rule.onNodeWithContentDescription(AdminAccessibility.ProductListPanel).assertDoesNotExist()
    }

    fun assertFilterBadge(count: Int) {
        val label = if (count == 0) "Filters" else "Filters ($count)"
        rule.onNode(filtersToggleMatcher().and(hasText(label))).assertExists()
    }
}

internal class ProductsRobot(
    private val rule: ComposeContentTestRule
) {
    fun openFilters() {
        rule.onNode(filtersToggleMatcher()).performClick()
        rule.waitUntilExists(hasContentDescription(AdminAccessibility.ProductNameFilter))
    }

    fun closeFilters() {
        rule.onNode(filtersToggleMatcher()).performClick()
        rule.waitUntilDoesNotExist(hasContentDescription(AdminAccessibility.ProductNameFilter))
    }

    fun search(query: String) {
        replaceText(AdminAccessibility.ProductNameFilter, query)
    }

    fun selectActiveFilter(filter: ProductActiveFilter) {
        rule.onNodeWithContentDescription(AdminAccessibility.productActiveFilter(filter)).performClick()
    }

    fun selectCategory(category: ProductCategory?) {
        rule.onNodeWithContentDescription(AdminAccessibility.productCategoryFilter(category)).performClick()
    }

    fun selectMerchant(label: String) {
        rule.onNodeWithContentDescription(AdminAccessibility.productMerchantFilter(label)).performClick()
    }

    fun waitForRow(product: AdminProductFixture) {
        rule.waitUntilExists(productRowMatcher(product))
    }

    fun selectProduct(product: AdminProductFixture) {
        rule.onNode(productRowMatcher(product)).performClick()
    }

    fun waitUntilVisibleProducts(
        allProducts: List<AdminProductFixture>,
        expectedProducts: List<AdminProductFixture>
    ) {
        val expectedDescriptions = expectedProducts.map { it.rowDescription }.toSet()
        rule.waitUntil {
            allProducts.all { product ->
                val exists = rule.nodeExists(hasContentDescription(product.rowDescription))
                if (product.rowDescription in expectedDescriptions) exists else !exists
            }
        }
    }

    fun assertRowState(
        product: AdminProductFixture,
        stock: Int = product.stock,
        isActive: Boolean = product.isActive
    ) {
        rule.waitUntilExists(
            hasContentDescription(product.rowDescription)
                .and(hasStateDescription(product.rowState(stock = stock, isActive = isActive)))
        )
    }

    fun assertDetailShowsProduct(
        product: AdminProductFixture,
        stock: Int = product.stock,
        isActive: Boolean = product.isActive
    ) {
        assertDetailContains(product.name)
        assertDetailContains(product.description)
        assertActivationButtonState(isActive)
        assertDetailContains(stock.toString())
    }

    fun assertDetailContains(vararg texts: String) {
        texts.forEach { text ->
            val matcher = panelTextMatcher(AdminAccessibility.ProductDetailPanel, text)
            rule.waitUntilExists(matcher, useUnmergedTree = true)
            rule.onNode(
                matcher,
                useUnmergedTree = true
            ).assertExists()
        }
    }

    fun assertEmptyState() {
        val listEmptyMatcher = panelTextMatcher(
            AdminAccessibility.ProductListPanel,
            "No products match the current filters."
        )
        val detailEmptyMatcher = panelTextMatcher(
            AdminAccessibility.ProductDetailPanel,
            "Select a product to inspect its details and operations."
        )

        rule.waitUntilExists(listEmptyMatcher, useUnmergedTree = true)
        rule.onNode(
            listEmptyMatcher,
            useUnmergedTree = true
        )
            .assertExists()
        rule.waitUntilExists(detailEmptyMatcher, useUnmergedTree = true)
        rule.onNode(
            detailEmptyMatcher,
            useUnmergedTree = true
        ).assertExists()
    }

    fun adjustStock(quantityChange: Int) {
        rule.onNodeWithContentDescription(stockAdjustmentAccessibilityDescription(quantityChange)).performClick()
    }

    fun toggleActivation(isActive: Boolean) {
        rule.onNode(
            panelTextMatcher(AdminAccessibility.ProductDetailPanel, activationButtonText(isActive))
                .and(hasClickAction())
        ).performClick()
    }

    fun assertActivationButtonState(isActive: Boolean) {
        val matcher = hasContentDescription(activationButtonText(isActive))
            .and(hasAnyAncestor(hasContentDescription(AdminAccessibility.ProductDetailPanel)))
        rule.waitUntilExists(matcher)
        rule.onNode(matcher).assertExists()
    }

    private fun replaceText(description: String, query: String) {
        rule.onNodeWithContentDescription(description).performTextClearance()
        if (query.isNotEmpty()) {
            rule.onNodeWithContentDescription(description).performTextInput(query)
        }
        rule.waitForIdle()
    }
}

internal class OrdersRobot(
    private val rule: ComposeContentTestRule
) {
    fun openFilters() {
        rule.onNode(filtersToggleMatcher()).performClick()
        rule.waitUntilExists(hasContentDescription(AdminAccessibility.OrderIdFilter))
    }

    fun closeFilters() {
        rule.onNode(filtersToggleMatcher()).performClick()
        rule.waitUntilDoesNotExist(hasContentDescription(AdminAccessibility.OrderIdFilter))
    }

    fun searchOrderId(query: String) {
        replaceText(AdminAccessibility.OrderIdFilter, query)
    }

    fun selectOrderStatusFilter(status: OrderStatus?) {
        rule.onNodeWithContentDescription(AdminAccessibility.orderStatusFilter(status)).performClick()
    }

    fun selectSubOrderStatusFilter(status: OrderStatus?) {
        rule.onNodeWithContentDescription(AdminAccessibility.subOrderStatusFilter(status)).performClick()
    }

    fun selectMerchant(label: String) {
        rule.onNodeWithContentDescription(AdminAccessibility.orderMerchantFilter(label)).performClick()
    }

    fun waitForRow(order: AdminOrderFixture) {
        rule.waitUntilExists(orderRowMatcher(order))
    }

    fun selectOrder(order: AdminOrderFixture) {
        rule.onNode(orderRowMatcher(order)).performClick()
    }

    fun waitUntilVisibleOrders(
        allOrders: List<AdminOrderFixture>,
        expectedOrders: List<AdminOrderFixture>
    ) {
        val expectedDescriptions = expectedOrders.map { it.rowDescription }.toSet()
        rule.waitUntil {
            allOrders.all { order ->
                val exists = rule.nodeExists(hasContentDescription(order.rowDescription))
                if (order.rowDescription in expectedDescriptions) exists else !exists
            }
        }
    }

    fun assertRowState(
        order: AdminOrderFixture,
        status: OrderStatus = order.initialStatus
    ) {
        rule.waitUntilExists(
            hasContentDescription(order.rowDescription)
                .and(hasStateDescription(order.rowState(status = status)))
        )
    }

    fun assertDetailShowsOrder(
        order: AdminOrderFixture,
        status: OrderStatus = order.initialStatus
    ) {
        assertDetailContains(order.title)
        assertDetailContains(order.characterName)
        assertStatusMetric(status)
    }

    fun assertStatusMetric(status: OrderStatus) {
        rule.onNode(
            hasText(status.labelize())
                .and(hasAnySibling(hasText("Status")))
                .and(hasAnyAncestor(hasContentDescription(AdminAccessibility.OrderDetailPanel))),
            useUnmergedTree = true
        ).assertExists()
    }

    fun assertDetailContains(vararg texts: String) {
        texts.forEach { text ->
            val matcher = panelTextMatcher(AdminAccessibility.OrderDetailPanel, text)
            rule.waitUntilExists(matcher, useUnmergedTree = true)
            rule.onNode(
                matcher,
                useUnmergedTree = true
            ).assertExists()
        }
    }

    fun assertSubOrderState(
        subOrder: AdminSubOrderFixture,
        status: OrderStatus = subOrder.initialStatus
    ) {
        rule.waitUntilExists(
            hasContentDescription(subOrder.description)
                .and(hasStateDescription(subOrder.state(status = status)))
        )
    }

    fun assertEmptyState() {
        val listEmptyMatcher = panelTextMatcher(
            AdminAccessibility.OrderListPanel,
            "No orders match the current filters."
        )
        val detailEmptyMatcher = panelTextMatcher(
            AdminAccessibility.OrderDetailPanel,
            "Select an order to inspect the hierarchy and update order and sub-order status."
        )

        rule.waitUntilExists(listEmptyMatcher, useUnmergedTree = true)
        rule.onNode(
            listEmptyMatcher,
            useUnmergedTree = true
        )
            .assertExists()
        rule.waitUntilExists(detailEmptyMatcher, useUnmergedTree = true)
        rule.onNode(
            detailEmptyMatcher,
            useUnmergedTree = true
        ).assertExists()
    }

    fun updateOrderStatus(status: OrderStatus) {
        rule.onNodeWithContentDescription(orderStatusAccessibilityDescription(status)).performClick()
    }

    fun updateSubOrderStatus(subOrder: AdminSubOrderFixture, status: OrderStatus) {
        rule.onNodeWithContentDescription(subOrderStatusAccessibilityDescription(subOrder.id, status)).performClick()
    }

    private fun replaceText(description: String, query: String) {
        rule.onNodeWithContentDescription(description).performTextClearance()
        if (query.isNotEmpty()) {
            rule.onNodeWithContentDescription(description).performTextInput(query)
        }
        rule.waitForIdle()
    }
}

internal fun ComposeContentTestRule.waitUntilExists(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 5_000,
    useUnmergedTree: Boolean = false
) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodes(matcher, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
    }
}

private fun ProductsRobot.productRowMatcher(product: AdminProductFixture): SemanticsMatcher =
    hasContentDescription(product.rowDescription)
        .and(hasStateDescription(product.rowState()))
        .and(hasClickAction())

private fun OrdersRobot.orderRowMatcher(order: AdminOrderFixture): SemanticsMatcher =
    hasContentDescription(order.rowDescription)
        .and(hasStateDescription(order.rowState()))
        .and(hasClickAction())

private fun workspaceTabMatcher(label: String): SemanticsMatcher =
    hasText(label).and(hasClickAction())

private fun filtersToggleMatcher(): SemanticsMatcher =
    hasText("Filters", substring = true).and(hasClickAction())

private fun panelTextMatcher(panelDescription: String, text: String): SemanticsMatcher =
    hasText(text).and(hasAnyAncestor(hasContentDescription(panelDescription)))

private fun ComposeContentTestRule.nodeExists(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean = false
): Boolean = onAllNodes(matcher, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isNotEmpty()

private fun activationButtonText(isActive: Boolean): String =
    if (isActive) "Deactivate product" else "Activate product"

private fun OrderStatus.labelize(): String =
    name.lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { segment ->
            segment.replaceFirstChar { character ->
                if (character.isLowerCase()) {
                    character.titlecase()
                } else {
                    character.toString()
                }
            }
        }
