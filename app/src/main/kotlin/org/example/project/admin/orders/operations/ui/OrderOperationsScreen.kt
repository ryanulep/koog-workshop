@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin.orders.operations.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.example.project.admin.orders.operations.OrderAdminUiState
import org.example.project.admin.shared.ui.AdminAccessibility
import org.example.project.admin.shared.ui.AdminChromeSectionPadding
import org.example.project.admin.shared.ui.AdminMetric
import org.example.project.admin.shared.ui.AdminMetricsRow
import org.example.project.admin.shared.ui.AdminScreenPadding
import org.example.project.admin.shared.ui.AdminSectionSpacing
import org.example.project.admin.shared.ui.DetailBlock
import org.example.project.admin.shared.ui.FilterGroup
import org.example.project.admin.shared.ui.InlineErrorCard
import org.example.project.admin.shared.ui.PanelEmptyState
import org.example.project.admin.shared.ui.PanelHeader
import org.example.project.admin.shared.ui.ToolbarTextFilter
import org.example.project.admin.shared.ui.formatAdminInstant
import org.example.project.admin.shared.ui.formatAmount
import org.example.project.admin.shared.ui.labelize
import org.example.project.domain.admin.AdminOrderDetail
import org.example.project.domain.admin.AdminOrderHistoryEvent
import org.example.project.domain.admin.AdminOrderItemDetail
import org.example.project.domain.admin.AdminSubOrderDetail
import org.example.project.domain.admin.OrderListItem
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId

internal fun orderActiveFilterCount(uiState: OrderAdminUiState): Int =
    orderSecondaryFilterSummaries(uiState).size +
        (if (uiState.filter.orderIdQuery.isNotBlank()) 1 else 0) +
        (if (uiState.filter.orderStatus != null) 1 else 0)

@Composable
internal fun OrderFilterContent(
    uiState: OrderAdminUiState,
    onUpdateOrderIdQuery: (String) -> Unit,
    onUpdateOrderStatusFilter: (OrderStatus?) -> Unit,
    onUpdateSubOrderStatusFilter: (OrderStatus?) -> Unit,
    onUpdateMerchant: (MerchantId?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)) {
        ToolbarTextFilter(
            value = uiState.filter.orderIdQuery,
            onValueChange = onUpdateOrderIdQuery,
            label = "Order ID",
            placeholder = "Filter by order ID",
            accessibilityDescription = AdminAccessibility.OrderIdFilter
        )
        Text(
            text = "Filter by order status",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val orderStatusOptions: List<Pair<OrderStatus?, String>> =
                listOf(null to "All") + OrderStatus.entries.map { it to it.labelize() }
            orderStatusOptions.forEach { (status, label) ->
                FilterChip(
                    modifier = Modifier.semantics {
                        contentDescription = AdminAccessibility.orderStatusFilter(status)
                    },
                    selected = status == uiState.filter.orderStatus,
                    onClick = { onUpdateOrderStatusFilter(status) },
                    label = { Text(label) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FilterGroup(
                title = "Sub-order status",
                options = persistentListOf<Pair<String, OrderStatus?>>("All" to null)
                    .addAll(OrderStatus.entries.map { it.labelize() to it }),
                selected = uiState.filter.subOrderStatus,
                onSelect = onUpdateSubOrderStatusFilter,
                optionContentDescription = { _, value -> AdminAccessibility.subOrderStatusFilter(value) },
                modifier = Modifier.weight(1f)
            )
            FilterGroup(
                title = "Merchant",
                options = persistentListOf<Pair<String, MerchantId?>>("All" to null)
                    .addAll(uiState.merchants.map { it.name to it.id }),
                selected = uiState.filter.merchantId,
                onSelect = onUpdateMerchant,
                optionContentDescription = { label, _ -> AdminAccessibility.orderMerchantFilter(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OrderOperationsScreen(
    uiState: OrderAdminUiState,
    onSelectOrder: (org.example.project.domain.shared.OrderId) -> Unit,
    onUpdateOrderStatus: (org.example.project.domain.shared.OrderId, OrderStatus) -> Unit,
    onUpdateSubOrderStatus: (org.example.project.domain.shared.SubOrderId, OrderStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AdminScreenPadding, vertical = AdminChromeSectionPadding),
        verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
    ) {
        uiState.errorMessage?.let { message ->
            InlineErrorCard(message = message)
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
        ) {
            OrderListPanel(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight(),
                orders = uiState.orders,
                selectedOrderId = uiState.selectedOrderId,
                onSelectOrder = onSelectOrder
            )

            OrderDetailPanel(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                order = uiState.selectedOrder,
                onUpdateOrderStatus = onUpdateOrderStatus,
                onUpdateSubOrderStatus = onUpdateSubOrderStatus
            )
        }
    }
}

@Composable
private fun OrderListPanel(
    modifier: Modifier,
    orders: PersistentList<org.example.project.domain.admin.OrderListItem>,
    selectedOrderId: org.example.project.domain.shared.OrderId?,
    onSelectOrder: (org.example.project.domain.shared.OrderId) -> Unit
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = AdminAccessibility.OrderListPanel
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AdminScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
        ) {
            PanelHeader(
                title = "Orders",
                subtitle = "${orders.size} matching orders"
            )

            if (orders.isEmpty()) {
                PanelEmptyState(message = "No orders match the current filters.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
                ) {
                    items(
                        items = orders,
                        key = { order -> order.orderId.value }
                    ) { order ->
                        OrderRow(
                            order = order,
                            selected = order.orderId == selectedOrderId,
                            onClick = { onSelectOrder(order.orderId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(
    order: org.example.project.domain.admin.OrderListItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = order.orderRowAccessibilityDescription()
            stateDescription = order.orderRowAccessibilityState()
        },
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Order ${order.orderId.value}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = order.characterName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onClick,
                    label = { Text(order.status.labelize()) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.totalPrice.formatAmount(order.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${order.merchantCount} merchants",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = order.createdAt.formatAdminInstant(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrderDetailPanel(
    modifier: Modifier,
    order: org.example.project.domain.admin.AdminOrderDetail?,
    onUpdateOrderStatus: (org.example.project.domain.shared.OrderId, OrderStatus) -> Unit,
    onUpdateSubOrderStatus: (org.example.project.domain.shared.SubOrderId, OrderStatus) -> Unit
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = AdminAccessibility.OrderDetailPanel
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        when (order) {
            null -> PanelEmptyState(
                modifier = Modifier.fillMaxSize(),
                message = "Select an order to inspect the hierarchy and update order and sub-order status."
            )

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AdminScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PanelHeader(
                        title = "Order ${order.order.id.value}",
                        subtitle = order.characterName
                    )

                    AdminMetricsRow(
                        persistentListOf(
                            AdminMetric("Status", order.order.status.labelize()),
                            AdminMetric("Total", order.order.totalPrice.formatAmount(order.currencyCode)),
                            AdminMetric("Sub-orders", order.subOrders.size.toString())
                        )
                    )
                    AdminMetricsRow(
                        persistentListOf(
                            AdminMetric("Created", order.order.createdAt.formatAdminInstant()),
                            AdminMetric("Updated", order.order.updatedAt.formatAdminInstant())
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Update order status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderStatus.entries.forEach { status ->
                                FilterChip(
                                    modifier = Modifier.semantics {
                                        contentDescription = orderStatusAccessibilityDescription(status)
                                    },
                                    selected = order.order.status == status,
                                    onClick = { onUpdateOrderStatus(order.order.id, status) },
                                    label = { Text(status.labelize()) }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Sub-orders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        order.subOrders.forEach { subOrder ->
                            SubOrderCard(
                                detail = subOrder,
                                onUpdateStatus = onUpdateSubOrderStatus
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (order.history.isEmpty()) {
                            DetailBlock(
                                title = "No events",
                                body = "No history events were recorded for this order."
                            )
                        } else {
                            order.history.forEach { event ->
                                HistoryEventCard(event = event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubOrderCard(
    detail: org.example.project.domain.admin.AdminSubOrderDetail,
    onUpdateStatus: (org.example.project.domain.shared.SubOrderId, OrderStatus) -> Unit
) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = detail.subOrderAccessibilityDescription()
            stateDescription = detail.subOrder.status.labelize()
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = detail.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sub-order ${detail.subOrder.id.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(detail.subOrder.status.labelize()) }
                )
            }

            AdminMetricsRow(
                persistentListOf(
                    AdminMetric("Shipping", detail.shippingMethodName),
                    AdminMetric(
                        "Shipping cost",
                        detail.subOrder.shippingCost.formatAmount(detail.shippingCostCurrencyCode)
                    ),
                    AdminMetric(
                        "Merchant total",
                        detail.subOrder.merchantTotalPrice.formatAmount(detail.shippingCostCurrencyCode)
                    )
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Update status",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrderStatus.entries.forEach { status ->
                        FilterChip(
                            modifier = Modifier.semantics {
                                contentDescription =
                                    subOrderStatusAccessibilityDescription(detail.subOrder.id, status)
                            },
                            selected = detail.subOrder.status == status,
                            onClick = { onUpdateStatus(detail.subOrder.id, status) },
                            label = { Text(status.labelize()) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                detail.items.forEach { item ->
                    OrderItemRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: org.example.project.domain.admin.AdminOrderItemDetail) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${item.productCategory.labelize()} · ${item.merchantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.subtotal.formatAmount(item.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "${item.item.quantity} x ${item.unitPrice.formatAmount(item.currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.productDescription?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryEventCard(event: org.example.project.domain.admin.AdminOrderHistoryEvent) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = event.timestamp.formatAdminInstant(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun orderSecondaryFilterSummaries(uiState: OrderAdminUiState): List<String> =
    listOfNotNull(
        uiState.filter.subOrderStatus?.let { status -> "Sub-order: ${status.labelize()}" },
        uiState.filter.merchantId?.let { merchantId ->
            val merchantName = uiState.merchants.firstOrNull { merchant -> merchant.id == merchantId }?.name
                ?: "Selected merchant"
            "Merchant: $merchantName"
        }
    )

private fun org.example.project.domain.admin.OrderListItem.orderRowAccessibilityDescription(): String =
    "Order ${orderId.value} for $characterName"

private fun org.example.project.domain.admin.OrderListItem.orderRowAccessibilityState(): String =
    "${status.labelize()}, $merchantCount merchants, ${totalPrice.formatAmount(currencyCode)}"

private fun org.example.project.domain.admin.AdminSubOrderDetail.subOrderAccessibilityDescription(): String =
    "Sub-order ${subOrder.id.value} for $merchantName"

private fun orderStatusAccessibilityDescription(status: OrderStatus): String =
    "Set order status to ${status.labelize()}"

private fun subOrderStatusAccessibilityDescription(
    subOrderId: org.example.project.domain.shared.SubOrderId,
    status: OrderStatus
): String = "Set sub-order ${subOrderId.value} status to ${status.labelize()}"
