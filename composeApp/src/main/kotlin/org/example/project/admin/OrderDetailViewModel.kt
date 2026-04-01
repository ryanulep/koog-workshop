@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import org.example.project.domain.id.OrderId
import org.example.project.domain.id.OrderItemId
import org.example.project.domain.model.AdminOrderDetail
import org.example.project.service.AdminDashboardService

class OrderDetailViewModel(
    private val dashboardService: AdminDashboardService
) : ViewModel() {

    private val loadVersion = AtomicLong(0L)
    private val _uiState = MutableStateFlow<AdminOrderDetailUiState>(AdminOrderDetailUiState.Loading)

    val uiState: StateFlow<AdminOrderDetailUiState> = _uiState.asStateFlow()

    fun startLoading() {
        loadVersion.incrementAndGet()
        _uiState.value = AdminOrderDetailUiState.Loading
    }

    suspend fun loadOrderDetail(orderId: OrderId) {
        val version = loadVersion.incrementAndGet()
        val previousSelection = (_uiState.value as? AdminOrderDetailUiState.Ready)?.selectedItemId
        _uiState.value = AdminOrderDetailUiState.Loading

        val nextState = try {
            dashboardService.loadOrderDetailsOrNull(orderId).toUiState(orderId, previousSelection)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AdminOrderDetailUiState.Error(throwable.message ?: "Unable to load order details.")
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    fun selectItem(itemId: OrderItemId) {
        val current = _uiState.value as? AdminOrderDetailUiState.Ready ?: return
        if (current.detail.containsItem(itemId)) {
            _uiState.value = current.copy(selectedItemId = itemId)
        }
    }

    companion object {
        fun factory(dashboardService: AdminDashboardService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == OrderDetailViewModel::class) {
                        return OrderDetailViewModel(dashboardService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }

    private fun AdminOrderDetail?.toUiState(
        orderId: OrderId,
        selectedItemId: OrderItemId?
    ): AdminOrderDetailUiState =
        if (this == null) {
            AdminOrderDetailUiState.Error("Order ${orderId.value} was not found.")
        } else {
            AdminOrderDetailUiState.Ready(
                detail = this,
                selectedItemId = selectedItemId?.takeIf { containsItem(it) }
            )
        }

    private fun AdminOrderDetail.containsItem(itemId: OrderItemId): Boolean =
        subOrders.any { subOrder ->
            subOrder.items.any { item -> item.item.id == itemId }
        }
}
