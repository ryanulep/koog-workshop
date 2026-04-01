@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.domain.id.OrderItemId
import org.example.project.domain.model.AdminOrderDetail
import org.example.project.domain.model.AdminOrderHistoryEvent
import org.example.project.domain.model.AdminOrderItemDetail
import org.example.project.domain.model.AdminSubOrderDetail
import org.example.project.domain.model.RecentOrderSummary
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

private val adminDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

@Composable
fun AdminTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            if (onRefresh != null) {
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onOrderClick: (RecentOrderSummary) -> Unit
) {
    when (uiState) {
        DashboardUiState.Uninitialized,
        DashboardUiState.Loading -> LoadingCard(
            title = "Loading order history",
            body = "Reading the latest order activity."
        )

        is DashboardUiState.Error -> ErrorCard(
            title = "Order history failed to load",
            message = uiState.message,
            onRefresh = onRefresh
        )

        is DashboardUiState.Ready -> OrderHistoryCard(
            orders = uiState.orders,
            onOrderClick = onOrderClick
        )
    }
}

@Composable
fun OrderDetailScreen(
    uiState: AdminOrderDetailUiState,
    onRefresh: () -> Unit,
    onItemClick: (AdminOrderItemDetail) -> Unit
) {
    when (uiState) {
        AdminOrderDetailUiState.Loading -> LoadingCard(
            title = "Loading order details",
            body = "Reading the full history for this order."
        )

        is AdminOrderDetailUiState.Error -> ErrorCard(
            title = "Order details failed to load",
            message = uiState.message,
            onRefresh = onRefresh
        )

        is AdminOrderDetailUiState.Ready -> OrderDetailContent(
            detail = uiState.detail,
            selectedItemId = uiState.selectedItemId,
            onItemClick = onItemClick
        )
    }
}

@Composable
fun LoadingCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorCard(
    title: String,
    message: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            OutlinedButton(onClick = onRefresh) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    orders: List<RecentOrderSummary>,
    onOrderClick: (RecentOrderSummary) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No orders have been placed yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(
                    items = orders,
                    key = { _, order -> order.orderId.value }
                ) { _, order ->
                    OrderHistoryRow(
                        order = order,
                        onClick = { onOrderClick(order) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryRow(
    order: RecentOrderSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Order ${order.orderId.value}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = order.characterName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text(order.status.name) }
                    )
                    Text(
                        text = order.totalPrice.formatAmount(order.totalCurrencyCode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
private fun OrderDetailSummaryCard(detail: AdminOrderDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = detail.order.id.value.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = detail.characterName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(detail.order.status.name) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailMetric(
                    label = "Total",
                    value = detail.order.totalPrice.formatAmount(detail.currencyCode)
                )
                DetailMetric(
                    label = "Created",
                    value = detail.order.createdAt.formatAdminInstant()
                )
                DetailMetric(
                    label = "Updated",
                    value = detail.order.updatedAt.formatAdminInstant()
                )
            }
        }
    }
}

@Composable
private fun OrderSubOrdersSection(
    detail: AdminOrderDetail,
    onItemClick: (AdminOrderItemDetail) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Sub-orders",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        detail.subOrders.forEach { subOrderDetail ->
            SubOrderCard(
                detail = subOrderDetail,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun SubOrderCard(
    detail: AdminSubOrderDetail,
    onItemClick: (AdminOrderItemDetail) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    label = { Text(detail.subOrder.status.name) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailMetric(
                    label = "Shipping",
                    value = detail.shippingMethodName
                )
                DetailMetric(
                    label = "Shipping cost",
                    value = detail.subOrder.shippingCost.formatAmount(detail.shippingCostCurrencyCode)
                )
                DetailMetric(
                    label = "Merchant total",
                    value = detail.subOrder.merchantTotalPrice.formatAmount(detail.shippingCostCurrencyCode)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                detail.items.forEach { item ->
                    OrderItemRow(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(
    item: AdminOrderItemDetail,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.productCategory.labelize(),
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
        }
    }
}

@Composable
private fun ItemSnapshotCard(
    detail: AdminOrderDetail,
    item: AdminOrderItemDetail,
    parentSubOrder: AdminSubOrderDetail?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = detail.characterName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(item.productCategory.labelize()) }
                )
            }

            val productDescription = item.productDescription
            if (!productDescription.isNullOrBlank()) {
                Text(
                    text = productDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailMetric(
                    label = "Quantity",
                    value = item.item.quantity.toString()
                )
                DetailMetric(
                    label = "Unit price",
                    value = item.unitPrice.formatAmount(item.currencyCode)
                )
                DetailMetric(
                    label = "Subtotal",
                    value = item.subtotal.formatAmount(item.currencyCode)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailMetric(
                    label = "Merchant",
                    value = item.merchantName
                )
                DetailMetric(
                    label = "Order",
                    value = "Order ${detail.order.id.value}"
                )
                DetailMetric(
                    label = "Sub-order",
                    value = parentSubOrder?.subOrder?.id?.value?.toString() ?: "Unknown"
                )
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    detail: AdminOrderDetail,
    selectedItemId: OrderItemId?,
    onItemClick: (AdminOrderItemDetail) -> Unit
) {
    val selectedItem = detail.findItemOrNull(selectedItemId)
    val parentSubOrder = detail.findSubOrderForItemOrNull(selectedItemId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OrderDetailSummaryCard(detail = detail)
        OrderSubOrdersSection(
            detail = detail,
            onItemClick = onItemClick
        )

        Text(
            text = "Item details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (selectedItem != null) {
            ItemSnapshotCard(
                detail = detail,
                item = selectedItem,
                parentSubOrder = parentSubOrder
            )
        } else {
            ItemDetailHintCard()
        }

        OrderHistorySection(detail = detail)
    }
}

@Composable
private fun ItemDetailHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select an item to inspect its snapshot.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrderHistorySection(detail: AdminOrderDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Order history",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (detail.history.isEmpty()) {
            Text(
                text = "No history events were recorded for this order.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            detail.history.forEach { event ->
                HistoryEventCard(event = event)
            }
        }
    }
}

@Composable
private fun HistoryEventCard(
    event: AdminOrderHistoryEvent
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
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

@Composable
private fun DetailMetric(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun Long.formatAmount(currencyCode: String): String = "$this $currencyCode"

private fun String.labelize(): String =
    lowercase().replace('_', ' ')
        .split(' ')
        .joinToString(" ") { segment ->
            segment.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }

private fun Instant.formatAdminInstant(): String =
    adminDateFormatter.format(JavaInstant.ofEpochMilli(toEpochMilliseconds()))

private fun AdminOrderDetail.findItemOrNull(itemId: OrderItemId?): AdminOrderItemDetail? =
    itemId?.let { selectedItemId ->
        subOrders.asSequence()
            .flatMap { subOrder -> subOrder.items.asSequence() }
            .firstOrNull { item -> item.item.id == selectedItemId }
    }

private fun AdminOrderDetail.findSubOrderForItemOrNull(itemId: OrderItemId?): AdminSubOrderDetail? =
    itemId?.let { selectedItemId ->
        subOrders.firstOrNull { subOrder ->
            subOrder.items.any { item -> item.item.id == selectedItemId }
        }
    }
