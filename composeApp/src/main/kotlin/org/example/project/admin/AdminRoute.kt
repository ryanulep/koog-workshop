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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.example.project.domain.id.OrderId
import org.example.project.domain.id.OrderItemId
import org.example.project.domain.model.AdminOrderDetail
import org.example.project.service.AdminDashboardService

@Composable
fun AdminRoute(dashboardService: AdminDashboardService) {
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(dashboardService))
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<AdminScreen>(AdminScreen.OrderList) }
    var selectedOrderDetail by remember { mutableStateOf<AdminOrderDetail?>(null) }
    var orderDetailLoading by remember { mutableStateOf(false) }
    var orderDetailError by remember { mutableStateOf<String?>(null) }

    suspend fun loadOrderDetail(orderId: OrderId, force: Boolean = false) {
        if (!force && selectedOrderDetail?.order?.id == orderId) {
            return
        }

        orderDetailLoading = true
        orderDetailError = null
        if (force || selectedOrderDetail?.order?.id != orderId) {
            selectedOrderDetail = null
        }

        try {
            selectedOrderDetail = dashboardViewModel.loadOrderDetails(orderId)
            if (selectedOrderDetail == null) {
                orderDetailError = "Order ${orderId.value} was not found."
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            orderDetailError = throwable.message ?: "Unable to load order details."
        } finally {
            orderDetailLoading = false
        }
    }

    fun refreshCurrentScreen() = coroutineScope.launch {
        dashboardViewModel.loadOrderHistory()

        when (val current = screen) {
            AdminScreen.OrderList -> Unit
            is AdminScreen.OrderDetail -> loadOrderDetail(current.orderId, force = true)
            is AdminScreen.ItemDetail -> loadOrderDetail(current.orderId, force = true)
        }
    }

    LaunchedEffect(Unit) {
        dashboardViewModel.loadOrderHistory()
    }

    LaunchedEffect(screen) {
        when (val current = screen) {
            AdminScreen.OrderList -> {
                selectedOrderDetail = null
                orderDetailError = null
                orderDetailLoading = false
            }

            is AdminScreen.OrderDetail -> {
                loadOrderDetail(current.orderId)
            }

            is AdminScreen.ItemDetail -> {
                loadOrderDetail(current.orderId)
            }
        }
    }

    val topBarTitle = when (val current = screen) {
        AdminScreen.OrderList -> "Order history"
        is AdminScreen.OrderDetail -> "Order details"
        is AdminScreen.ItemDetail -> "Item details"
    }

    val onBack = when (val current = screen) {
        AdminScreen.OrderList -> null
        is AdminScreen.OrderDetail -> ({ screen = AdminScreen.OrderList })
        is AdminScreen.ItemDetail -> ({ screen = AdminScreen.OrderDetail(current.orderId) })
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
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    AdminTopBar(
                        title = topBarTitle,
                        onBack = onBack,
                        onRefresh = ::refreshCurrentScreen
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (val current = screen) {
                        AdminScreen.OrderList -> OrderHistoryScreen(
                            uiState = dashboardState,
                            onRefresh = ::refreshCurrentScreen,
                            onOrderClick = { order ->
                                screen = AdminScreen.OrderDetail(order.orderId)
                            }
                        )

                        is AdminScreen.OrderDetail -> {
                            when {
                                orderDetailLoading && selectedOrderDetail == null -> LoadingCard(
                                    title = "Loading order details",
                                    body = "Reading the full history for this order."
                                )

                                orderDetailError != null -> ErrorCard(
                                    title = "Order details failed to load",
                                    message = orderDetailError ?: "Unable to load order details.",
                                    onRefresh = ::refreshCurrentScreen
                                )

                                selectedOrderDetail != null -> OrderDetailScreen(
                                    detail = selectedOrderDetail!!,
                                    onBack = { screen = AdminScreen.OrderList },
                                    onRefresh = ::refreshCurrentScreen,
                                    onItemClick = { item ->
                                        screen = AdminScreen.ItemDetail(
                                            orderId = current.orderId,
                                            itemId = item.item.id
                                        )
                                    }
                                )

                                else -> LoadingCard(
                                    title = "Loading order details",
                                    body = "Reading the full history for this order."
                                )
                            }
                        }

                        is AdminScreen.ItemDetail -> {
                            val detail = selectedOrderDetail
                            val item = detail
                                ?.subOrders
                                ?.asSequence()
                                ?.flatMap { subOrder -> subOrder.items.asSequence() }
                                ?.firstOrNull { it.item.id == current.itemId }

                            when {
                                orderDetailLoading && detail == null -> LoadingCard(
                                    title = "Loading item details",
                                    body = "Reading the order that contains this item."
                                )

                                orderDetailError != null -> ErrorCard(
                                    title = "Item details failed to load",
                                    message = orderDetailError ?: "Unable to load item details.",
                                    onRefresh = ::refreshCurrentScreen
                                )

                                detail != null && item != null -> OrderItemDetailScreen(
                                    detail = detail,
                                    item = item
                                )

                                detail != null -> ErrorCard(
                                    title = "Item not found",
                                    message = "The selected item is not part of the loaded order.",
                                    onRefresh = ::refreshCurrentScreen
                                )

                                else -> LoadingCard(
                                    title = "Loading item details",
                                    body = "Reading the order that contains this item."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface AdminScreen {
    data object OrderList : AdminScreen

    data class OrderDetail(val orderId: OrderId) : AdminScreen
    data class ItemDetail(val orderId: OrderId, val itemId: OrderItemId) : AdminScreen
}
