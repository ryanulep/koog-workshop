package org.example.project

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.example.project.domain.order.OrderStatus
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ComposeAppIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun productMutationsUpdateUiAndDatabase() {
        val scenario = createProductMutationScenario()

        rule.setContent {
            App(database = scenario.database)
        }

        val productRow = hasContentDescription(scenario.rowDescription)
            .and(hasStateDescription(scenario.rowState()))
            .and(hasClickAction())

        rule.waitUntilExists(productRow)
        rule.onNode(productRow).performClick()
        rule.onNodeWithContentDescription(stockAdjustmentAccessibilityDescription(5)).performClick()
        rule.waitUntilExists(
            hasContentDescription(scenario.rowDescription)
                .and(hasStateDescription(scenario.rowState(stock = scenario.initialStock + 5)))
        )

        rule.onNodeWithText("Deactivate product").performClick()
        rule.waitUntilExists(hasText("Activate product"))

        val product = scenario.loadProduct()
        assertEquals(scenario.initialStock + 5, product.stock)
        assertFalse(product.isActive)
    }

    @Test
    fun orderMutationsUpdateUiAndDatabase() {
        val scenario = createOrderMutationScenario()

        rule.setContent {
            App(database = scenario.database)
        }

        rule.onNodeWithText("Orders").performClick()

        val orderRow = hasContentDescription(scenario.orderRowDescription)
            .and(hasStateDescription(scenario.orderRowState(OrderStatus.PENDING)))
            .and(hasClickAction())

        rule.waitUntilExists(orderRow)
        rule.onNode(orderRow).performClick()

        rule.onNodeWithContentDescription(orderStatusAccessibilityDescription(OrderStatus.DELIVERED)).performClick()
        rule.waitUntilExists(
            hasContentDescription(scenario.orderRowDescription)
                .and(hasStateDescription(scenario.orderRowState(OrderStatus.DELIVERED)))
        )

        rule.onNodeWithContentDescription(
            subOrderStatusAccessibilityDescription(scenario.subOrderId, OrderStatus.SHIPPED)
        ).performClick()
        rule.waitUntilExists(
            hasContentDescription(scenario.subOrderDescription)
                .and(hasStateDescription("Shipped"))
        )

        val order = scenario.loadOrder()
        val subOrder = scenario.loadSubOrder()
        assertEquals(OrderStatus.DELIVERED, order.status)
        assertEquals(OrderStatus.SHIPPED, subOrder.status)
    }

    private fun ComposeContentTestRule.waitUntilExists(
        matcher: SemanticsMatcher,
        timeoutMillis: Long = 5_000
    ) {
        waitUntil(timeoutMillis) {
            onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
