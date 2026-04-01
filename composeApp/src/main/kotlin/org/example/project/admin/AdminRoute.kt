@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.example.project.domain.id.OrderId
import org.example.project.service.AdminDashboardService

@Composable
fun AdminRoute(dashboardService: AdminDashboardService) {
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(dashboardService))
    val orderDetailViewModel: OrderDetailViewModel = viewModel(factory = OrderDetailViewModel.factory(dashboardService))
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var destination by remember { mutableStateOf<AdminDestination>(AdminDestination.OrderList) }

    LaunchedEffect(Unit) {
        dashboardViewModel.loadOrderHistory()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            when (val current = destination) {
                AdminDestination.OrderList -> {
                    val refreshOrderHistory: () -> Unit = {
                        coroutineScope.launch {
                            dashboardViewModel.loadOrderHistory()
                        }
                    }

                    AdminScreenScaffold(
                        title = "Order history",
                        onRefresh = refreshOrderHistory
                    ) {
                        OrderHistoryScreen(
                            uiState = dashboardState,
                            onRefresh = refreshOrderHistory,
                            onOrderClick = { order ->
                                orderDetailViewModel.startLoading()
                                destination = AdminDestination.OrderDetail(order.orderId)
                            }
                        )
                    }
                }

                is AdminDestination.OrderDetail -> {
                    val orderDetailState by orderDetailViewModel.uiState.collectAsState()
                    val refreshOrderDetail: () -> Unit = {
                        coroutineScope.launch {
                            dashboardViewModel.loadOrderHistory()
                            orderDetailViewModel.loadOrderDetail(current.orderId)
                        }
                    }

                    LaunchedEffect(current.orderId) {
                        orderDetailViewModel.loadOrderDetail(current.orderId)
                    }

                    AdminScreenScaffold(
                        title = "Order details",
                        onBack = {
                            destination = AdminDestination.OrderList
                        },
                        onRefresh = refreshOrderDetail
                    ) {
                        OrderDetailScreen(
                            uiState = orderDetailState,
                            onRefresh = refreshOrderDetail,
                            onItemClick = { item ->
                                orderDetailViewModel.selectItem(item.item.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            AdminTopBar(
                title = title,
                onBack = onBack,
                onRefresh = onRefresh
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content()
        }
    }
}

private sealed interface AdminDestination {
    data object OrderList : AdminDestination

    data class OrderDetail(val orderId: OrderId) : AdminDestination
}
