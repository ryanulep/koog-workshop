@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.example.project.domain.admin.AdminOrderDetail
import org.example.project.domain.admin.AdminOrderHistoryEvent
import org.example.project.domain.admin.AdminOrderItemDetail
import org.example.project.domain.admin.AdminSubOrderDetail
import org.example.project.domain.admin.OrderAdminService
import org.example.project.domain.admin.OrderListItem
import org.example.project.domain.admin.ProductActiveFilter
import org.example.project.domain.admin.ProductAdminService
import org.example.project.domain.admin.ProductDetail
import org.example.project.domain.admin.ProductListItem
import org.example.project.domain.admin.ProductReviewSummary
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.SubOrderId
import java.util.Locale
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val adminDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

private val screenPadding = 16.dp
private val sectionSpacing = 10.dp
private val compactChromePadding = 12.dp
private val compactChromeButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
private val chromeSectionPadding = 12.dp

private enum class AdminWorkspaceTab(
    val title: String,
    val subtitle: String
) {
    Products(
        title = "Products",
        subtitle = "Catalog browsing, stock changes, and active-state control."
    ),
    Orders(
        title = "Orders",
        subtitle = "Operational triage, hierarchy inspection, and sub-order updates."
    )
}

@Composable
fun AdminRoute(
    productAdminService: ProductAdminService,
    orderAdminService: OrderAdminService
) {
    val productViewModel: ProductAdminViewModel =
        viewModel(factory = ProductAdminViewModel.factory(productAdminService))
    val orderViewModel: OrderAdminViewModel = viewModel(factory = OrderAdminViewModel.factory(orderAdminService))
    val productState by productViewModel.uiState.collectAsState()
    val orderState by orderViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        productViewModel.load()
        orderViewModel.refresh()
    }

    var selectedTab by rememberSaveable { mutableStateOf(AdminWorkspaceTab.Products) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AdminAppBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                onRefresh = {
                    when (selectedTab) {
                        AdminWorkspaceTab.Products -> productViewModel.refresh()
                        AdminWorkspaceTab.Orders -> orderViewModel.refresh()
                    }
                },
                activeFilterCount = when (selectedTab) {
                    AdminWorkspaceTab.Products ->
                        productSecondaryFilterSummaries(productState).size +
                                (if (productState.filter.nameQuery.isNotBlank()) 1 else 0) +
                                (if (productState.filter.activeFilter != ProductActiveFilter.ALL) 1 else 0)

                    AdminWorkspaceTab.Orders ->
                        orderSecondaryFilterSummaries(orderState).size +
                                (if (orderState.filter.orderIdQuery.isNotBlank()) 1 else 0) +
                                (if (orderState.filter.orderStatus != null) 1 else 0)
                },
                filterContent = {
                    when (selectedTab) {
                        AdminWorkspaceTab.Products -> {
                            Column(verticalArrangement = Arrangement.spacedBy(sectionSpacing)) {
                                ToolbarTextFilter(
                                    value = productState.filter.nameQuery,
                                    onValueChange = productViewModel::updateNameQuery,
                                    placeholder = "Search products"
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProductActiveFilter.entries.forEach { filter ->
                                        FilterChip(
                                            selected = filter == productState.filter.activeFilter,
                                            onClick = { productViewModel.updateActiveFilter(filter) },
                                            label = { Text(filter.labelize()) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    FilterGroup(
                                        title = "Category",
                                        options = persistentListOf<Pair<String, ProductCategory?>>("All" to null)
                                            .addAll(ProductCategory.entries.map { it.labelize() to it }),
                                        selected = productState.filter.category,
                                        onSelect = productViewModel::updateCategory,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterGroup(
                                        title = "Merchant",
                                        options = persistentListOf<Pair<String, MerchantId?>>("All" to null).addAll(
                                            productState.merchants.map { it.name to it.id }),
                                        selected = productState.filter.merchantId,
                                        onSelect = productViewModel::updateMerchant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        AdminWorkspaceTab.Orders -> {
                            Column(verticalArrangement = Arrangement.spacedBy(sectionSpacing)) {
                                ToolbarTextFilter(
                                    value = orderState.filter.orderIdQuery,
                                    onValueChange = orderViewModel::updateOrderIdQuery,
                                    placeholder = "Filter by order ID"
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
                                    val orderStatusOptions =
                                        persistentListOf(null to "All") + OrderStatus.entries.map { it to it.labelize() }
                                    orderStatusOptions.forEach { (status, label) ->
                                        FilterChip(
                                            selected = status == orderState.filter.orderStatus,
                                            onClick = { orderViewModel.updateOrderStatusFilter(status) },
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
                                        selected = orderState.filter.subOrderStatus,
                                        onSelect = orderViewModel::updateSubOrderStatusFilter,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterGroup(
                                        title = "Merchant",
                                        options = persistentListOf<Pair<String, MerchantId?>>("All" to null)
                                            .addAll(orderState.merchants.map { it.name to it.id }),
                                        selected = orderState.filter.merchantId,
                                        onSelect = orderViewModel::updateMerchant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            )

            when (selectedTab) {
                AdminWorkspaceTab.Products -> ProductOperationsScreen(
                    uiState = productState,
                    onSelectProduct = productViewModel::selectProduct,
                    onAdjustStock = productViewModel::adjustSelectedStock,
                    onSetActive = productViewModel::setSelectedProductActive
                )

                AdminWorkspaceTab.Orders -> OrderOperationsScreen(
                    uiState = orderState,
                    onSelectOrder = orderViewModel::selectOrder,
                    onUpdateOrderStatus = orderViewModel::updateOrderStatus,
                    onUpdateSubOrderStatus = orderViewModel::updateSubOrderStatus
                )
            }
        }
    }
}

@Composable
private fun AdminAppBar(
    selectedTab: AdminWorkspaceTab,
    onTabSelected: (AdminWorkspaceTab) -> Unit,
    onRefresh: () -> Unit,
    activeFilterCount: Int,
    filterContent: @Composable () -> Unit
) {
    var filtersOpen by rememberSaveable(selectedTab) { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 1.dp,
        shadowElevation = 3.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Fantasy Store Admin",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AdminWorkspaceTab.entries.forEach { tab ->
                        FilterChip(
                            selected = tab == selectedTab,
                            onClick = { onTabSelected(tab) },
                            label = { Text(tab.title) }
                        )
                    }
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onRefresh,
                    contentPadding = compactChromeButtonPadding
                ) { Text("Refresh") }

                val filtersLabel = if (activeFilterCount > 0 && !filtersOpen)
                    "Filters ($activeFilterCount)" else "Filters"
                FilterChip(
                    selected = filtersOpen || activeFilterCount > 0,
                    onClick = { filtersOpen = !filtersOpen },
                    label = { Text(filtersLabel) }
                )
            }

            AnimatedVisibility(
                visible = filtersOpen,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenPadding, vertical = 10.dp)
                    ) {
                        filterContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductOperationsScreen(
    uiState: ProductAdminUiState,
    onSelectProduct: (ProductId) -> Unit,
    onAdjustStock: (Int) -> Unit,
    onSetActive: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = screenPadding, vertical = chromeSectionPadding),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        uiState.errorMessage?.let { message ->
            InlineErrorCard(message = message)
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            ProductListPanel(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                products = uiState.products,
                selectedProductId = uiState.selectedProductId,
                onSelectProduct = onSelectProduct
            )

            ProductDetailPanel(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                product = uiState.selectedProduct,
                onAdjustStock = onAdjustStock,
                onSetActive = onSetActive
            )
        }
    }
}

@Composable
private fun ProductListPanel(
    modifier: Modifier,
    products: PersistentList<ProductListItem>,
    selectedProductId: ProductId?,
    onSelectProduct: (ProductId) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelHeader(
                title = "Catalog",
                subtitle = "${products.size} matching products"
            )

            if (products.isEmpty()) {
                PanelEmptyState(message = "No products match the current filters.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = products,
                        key = { product -> product.id.value }
                    ) { product ->
                        ProductRow(
                            product = product,
                            selected = product.id == selectedProductId,
                            onClick = { onSelectProduct(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductListItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = product.productRowAccessibilityDescription()
            stateDescription = product.productRowAccessibilityState()
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
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${product.category.labelize()} · ${product.merchantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(if (product.isActive) "Active" else "Inactive")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.price.formatAmount(product.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Stock ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = product.reviewSummary.toDisplayText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductDetailPanel(
    modifier: Modifier,
    product: ProductDetail?,
    onAdjustStock: (Int) -> Unit,
    onSetActive: (Boolean) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        when (product) {
            null -> PanelEmptyState(
                modifier = Modifier.fillMaxSize(),
                message = "Select a product to inspect its details and operations."
            )

            else -> {
                val imageUrl = product.imageUrl

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(screenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PanelHeader(
                        title = product.name,
                        subtitle = product.category.labelize()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.id.value.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            product.description?.takeIf { description -> description.isNotBlank() }
                                ?.let { description ->
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                        }

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(if (product.isActive) "Active" else "Inactive")
                            }
                        )
                    }

                    DetailMetricsRow(
                        DetailMetric("Price", product.price.formatAmount(product.currencyCode)),
                        DetailMetric("Stock", product.stock.toString()),
                        DetailMetric("Rarity", product.rarity.labelize())
                    )
                    DetailMetricsRow(
                        DetailMetric("Merchant", product.merchantName),
                        DetailMetric("Currency", "${product.currencyCode} (${product.currencySymbol})"),
                        DetailMetric("Reviews", product.reviewSummary.toDisplayText())
                    )
                    DetailMetricsRow(
                        DetailMetric("Created", product.createdAt.formatAdminInstant()),
                        DetailMetric("Updated", product.updatedAt.formatAdminInstant())
                    )

                    if (imageUrl != null) {
                        DetailBlock(
                            title = "Image URL",
                            body = imageUrl
                        )
                    }

                    if (product.categoryAttributes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Category fields",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            product.categoryAttributes.forEach { attribute ->
                                DetailBlock(
                                    title = attribute.label,
                                    body = attribute.value
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(-5, -1, 1, 5).forEach { quantityChange ->
                                FilledTonalButton(
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            stockAdjustmentAccessibilityDescription(quantityChange)
                                    },
                                    onClick = { onAdjustStock(quantityChange) }
                                ) {
                                    Text(
                                        text = if (quantityChange > 0) "+$quantityChange stock" else "$quantityChange stock"
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onSetActive(!product.isActive) }
                        ) {
                            Text(if (product.isActive) "Deactivate product" else "Activate product")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderOperationsScreen(
    uiState: OrderAdminUiState,
    onSelectOrder: (OrderId) -> Unit,
    onUpdateOrderStatus: (OrderId, OrderStatus) -> Unit,
    onUpdateSubOrderStatus: (SubOrderId, OrderStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = screenPadding, vertical = chromeSectionPadding),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        uiState.errorMessage?.let { message ->
            InlineErrorCard(message = message)
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
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
    orders: PersistentList<OrderListItem>,
    selectedOrderId: OrderId?,
    onSelectOrder: (OrderId) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
    order: OrderListItem,
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
    order: AdminOrderDetail?,
    onUpdateOrderStatus: (OrderId, OrderStatus) -> Unit,
    onUpdateSubOrderStatus: (SubOrderId, OrderStatus) -> Unit
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = modifier,
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
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(screenPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PanelHeader(
                            title = "Order ${order.order.id.value}",
                            subtitle = order.characterName
                        )

                        DetailMetricsRow(
                            DetailMetric("Status", order.order.status.labelize()),
                            DetailMetric("Total", order.order.totalPrice.formatAmount(order.currencyCode)),
                            DetailMetric("Sub-orders", order.subOrders.size.toString())
                        )
                        DetailMetricsRow(
                            DetailMetric("Created", order.order.createdAt.formatAdminInstant()),
                            DetailMetric("Updated", order.order.updatedAt.formatAdminInstant())
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
}

@Composable
private fun SubOrderCard(
    detail: AdminSubOrderDetail,
    onUpdateStatus: (SubOrderId, OrderStatus) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

            DetailMetricsRow(
                DetailMetric("Shipping", detail.shippingMethodName),
                DetailMetric(
                    "Shipping cost",
                    detail.subOrder.shippingCost.formatAmount(detail.shippingCostCurrencyCode)
                ),
                DetailMetric(
                    "Merchant total",
                    detail.subOrder.merchantTotalPrice.formatAmount(detail.shippingCostCurrencyCode)
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
private fun OrderItemRow(item: AdminOrderItemDetail) {
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

            item.productDescription?.takeIf { description -> description.isNotBlank() }?.let { description ->
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
private fun HistoryEventCard(event: AdminOrderHistoryEvent) {
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

@Composable
private fun PanelHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PanelEmptyState(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InlineErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun DetailBlock(
    title: String,
    body: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private data class DetailMetric(
    val label: String,
    val value: String
)

@Composable
private fun ToolbarTextFilter(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(placeholder) }
    )
}

@Composable
private fun DetailMetricsRow(vararg metrics: DetailMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        metrics.forEach { metric ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun <T> FilterGroup(
    title: String,
    options: PersistentList<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (label, value) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

private fun Long.formatAmount(currencyCode: String): String = "$this $currencyCode"

private fun ProductListItem.productRowAccessibilityDescription(): String =
    "Product $name from $merchantName"

private fun ProductListItem.productRowAccessibilityState(): String =
    "${if (isActive) "Active" else "Inactive"}, stock $stock"

private fun OrderListItem.orderRowAccessibilityDescription(): String =
    "Order ${orderId.value} for $characterName"

private fun OrderListItem.orderRowAccessibilityState(): String =
    "${status.labelize()}, $merchantCount merchants, ${totalPrice.formatAmount(currencyCode)}"

private fun AdminSubOrderDetail.subOrderAccessibilityDescription(): String =
    "Sub-order ${subOrder.id.value} for $merchantName"

private fun stockAdjustmentAccessibilityDescription(quantityChange: Int): String =
    if (quantityChange >= 0) {
        "Increase stock by $quantityChange"
    } else {
        "Decrease stock by ${-quantityChange}"
    }

private fun orderStatusAccessibilityDescription(status: OrderStatus): String =
    "Set order status to ${status.labelize()}"

private fun subOrderStatusAccessibilityDescription(
    subOrderId: SubOrderId,
    status: OrderStatus
): String = "Set sub-order ${subOrderId.value} status to ${status.labelize()}"

private fun Enum<*>.labelize(): String = name.labelize()

private fun ProductActiveFilter.labelize(): String =
    when (this) {
        ProductActiveFilter.ALL -> "All"
        ProductActiveFilter.ACTIVE -> "Active"
        ProductActiveFilter.INACTIVE -> "Inactive"
    }

private fun productSecondaryFilterSummaries(uiState: ProductAdminUiState): List<String> =
    listOfNotNull(
        uiState.filter.category?.let { category -> "Category: ${category.labelize()}" },
        uiState.filter.merchantId?.let { merchantId ->
            val merchantName = uiState.merchants.firstOrNull { merchant -> merchant.id == merchantId }?.name
                ?: "Selected merchant"
            "Merchant: $merchantName"
        }
    )

private fun orderSecondaryFilterSummaries(uiState: OrderAdminUiState): List<String> =
    listOfNotNull(
        uiState.filter.subOrderStatus?.let { status -> "Sub-order: ${status.labelize()}" },
        uiState.filter.merchantId?.let { merchantId ->
            val merchantName = uiState.merchants.firstOrNull { merchant -> merchant.id == merchantId }?.name
                ?: "Selected merchant"
            "Merchant: $merchantName"
        }
    )

private fun String.labelize(): String =
    lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { segment ->
            segment.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase()
                } else {
                    char.toString()
                }
            }
        }

private fun ProductReviewSummary.toDisplayText(): String =
    if (reviewCount == 0 || averageRating == null) {
        "No reviews yet"
    } else {
        "${String.format(Locale.US, "%.1f", averageRating)} / 5 from $reviewCount reviews"
    }

private fun kotlin.time.Instant.formatAdminInstant(): String =
    adminDateFormatter.format(JavaInstant.ofEpochMilli(toEpochMilliseconds()))
