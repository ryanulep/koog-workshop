package org.example.project.admin.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.Dependencies
import org.example.project.admin.orders.operations.OrderAdminViewModel
import org.example.project.admin.orders.operations.ui.OrderFilterContent
import org.example.project.admin.orders.operations.ui.OrderOperationsScreen
import org.example.project.admin.orders.operations.ui.orderActiveFilterCount
import org.example.project.admin.merchants.MerchantAdminViewModel
import org.example.project.admin.merchants.ui.MerchantOperationsScreen
import org.example.project.admin.products.ProductAdminViewModel
import org.example.project.admin.products.ui.ProductFilterContent
import org.example.project.admin.products.ui.ProductOperationsScreen
import org.example.project.admin.products.ui.productActiveFilterCount
import org.example.project.admin.shared.ui.AdminCompactChromeButtonPadding
import org.example.project.admin.shared.ui.AdminScreenPadding

private enum class AdminWorkspaceTab(val title: String) {
    Products("Products"),
    Merchants("Merchants"),
    Orders("Orders")
}

@Composable
fun AdminRoute(storeServices: Dependencies.StoreServices) {
    val productViewModel: ProductAdminViewModel =
        viewModel(factory = ProductAdminViewModel.factory(storeServices.productService))
    val merchantViewModel: MerchantAdminViewModel =
        viewModel(factory = MerchantAdminViewModel.factory(storeServices.merchantService))
    val orderViewModel: OrderAdminViewModel =
        viewModel(factory = OrderAdminViewModel.factory(storeServices.adminOrderService))
    val productState by productViewModel.uiState.collectAsState()
    val merchantState by merchantViewModel.uiState.collectAsState()
    val orderState by orderViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        productViewModel.refresh()
        merchantViewModel.refresh()
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
                        AdminWorkspaceTab.Merchants -> merchantViewModel.refresh()
                        AdminWorkspaceTab.Orders -> orderViewModel.refresh()
                    }
                },
                activeFilterCount = when (selectedTab) {
                    AdminWorkspaceTab.Products -> productActiveFilterCount(productState)
                    AdminWorkspaceTab.Merchants -> 0
                    AdminWorkspaceTab.Orders -> orderActiveFilterCount(orderState)
                }
            ) {
                when (selectedTab) {
                    AdminWorkspaceTab.Merchants -> {}
                    AdminWorkspaceTab.Products ->
                        ProductFilterContent(
                            uiState = productState,
                            onUpdateNameQuery = productViewModel::updateNameQuery,
                            onUpdateActiveFilter = productViewModel::updateActiveFilter,
                            onUpdateCategory = productViewModel::updateCategory,
                            onUpdateMerchant = productViewModel::updateMerchant
                        )

                    AdminWorkspaceTab.Orders -> OrderFilterContent(
                        uiState = orderState,
                        onUpdateOrderIdQuery = orderViewModel::updateOrderIdQuery,
                        onUpdateOrderStatusFilter = orderViewModel::updateOrderStatusFilter,
                        onUpdateSubOrderStatusFilter = orderViewModel::updateSubOrderStatusFilter,
                        onUpdateMerchant = orderViewModel::updateMerchant
                    )
                }
            }

            when (selectedTab) {
                AdminWorkspaceTab.Products -> ProductOperationsScreen(
                    uiState = productState,
                    onSelectProduct = productViewModel::selectProduct,
                    onAdjustStock = productViewModel::adjustSelectedStock,
                    onSetActive = productViewModel::setSelectedProductActive
                )

                AdminWorkspaceTab.Merchants -> MerchantOperationsScreen(
                    uiState = merchantState,
                    onSelectMerchant = merchantViewModel::selectMerchant,
                    onSetMerchantActive = merchantViewModel::setSelectedMerchantActive,
                    onSetShippingMethodActive = merchantViewModel::setShippingMethodActive,
                    onUpdateShippingAssignmentSelection =
                        merchantViewModel::updateShippingAssignmentSelection,
                    onSaveShippingAssignments = merchantViewModel::saveShippingAssignments
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
    filterContent: (@Composable () -> Unit)?
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
                    .padding(horizontal = AdminScreenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Fantasy Store Admin",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                val filtersLabel =
                    if (activeFilterCount > 0 && !filtersOpen) "Filters ($activeFilterCount)" else "Filters"

                FilterChip(
                    modifier = Modifier.semantics {
                        stateDescription = if (filtersOpen) "Expanded" else "Collapsed"
                    },
                    selected = filtersOpen || activeFilterCount > 0,
                    onClick = { filtersOpen = !filtersOpen },
                    label = { Text(filtersLabel) },
                    enabled = filterContent != null
                )

                OutlinedButton(
                    onClick = onRefresh,
                    contentPadding = AdminCompactChromeButtonPadding
                ) {
                    Text("Refresh")
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AdminWorkspaceTab.entries.forEach { tab ->
                        FilterChip(
                            selected = tab == selectedTab,
                            onClick = { onTabSelected(tab) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = filterContent != null && filtersOpen,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AdminScreenPadding, vertical = 10.dp)
                    ) {
                        filterContent?.invoke()
                    }
                }
            }
        }
    }
}
