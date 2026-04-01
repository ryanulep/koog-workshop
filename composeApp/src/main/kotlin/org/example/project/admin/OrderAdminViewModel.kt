@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.admin.OrderAdminService
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

class OrderAdminViewModel(
    private val orderAdminService: OrderAdminService
) : ViewModel() {

    private val loadVersion = AtomicLong(0L)
    private val _uiState = MutableStateFlow(OrderAdminUiState())

    val uiState: StateFlow<OrderAdminUiState> = _uiState.asStateFlow()

    fun refresh() = viewModelScope.launch {
        reload()
    }

    fun updateOrderIdQuery(query: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(orderIdQuery = query)
        )
        reload()
    }

    fun updateOrderStatusFilter(status: OrderStatus?) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(orderStatus = status)
        )
        reload()
    }

    fun updateOrderStatus(status: OrderStatus?) = updateOrderStatusFilter(status)

    fun updateOrderStatus(orderId: OrderId, status: OrderStatus) = viewModelScope.launch {
        updateOrderStatusInternal(orderId, status)
    }

    fun updateSubOrderStatusFilter(status: OrderStatus?) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(subOrderStatus = status)
        )
        reload()
    }

    fun updateMerchant(merchantId: MerchantId?) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(merchantId = merchantId)
        )
        reload()
    }

    fun selectOrder(orderId: OrderId) = viewModelScope.launch {
        selectOrderInternal(orderId)
    }

    fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus) = viewModelScope.launch {
        updateSubOrderStatusInternal(subOrderId, status)
    }

    private suspend fun selectOrderInternal(orderId: OrderId) {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(
            errorMessage = null,
            selectedOrderId = orderId,
            selectedOrder = current.selectedOrder?.takeIf { order -> order.order.id == orderId }
        )

        val nextState = try {
            val detail = orderAdminService.loadOrderDetailOrNull(orderId)
            if (detail == null) {
                current.copy(
                    errorMessage = "Order ${orderId.value} was not found.",
                    selectedOrderId = null,
                    selectedOrder = null
                )
            } else {
                current.copy(
                    errorMessage = null,
                    selectedOrderId = orderId,
                    selectedOrder = detail
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                errorMessage = throwable.message ?: "Unable to load order details."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    private suspend fun updateSubOrderStatusInternal(subOrderId: SubOrderId, status: OrderStatus) {
        val current = _uiState.value
        _uiState.value = current.copy(errorMessage = null)

        val success = try {
            orderAdminService.updateSubOrderStatus(subOrderId, status)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            _uiState.value = current.copy(
                errorMessage = throwable.message ?: "Unable to update sub-order status."
            )
            return
        }

        if (!success) {
            _uiState.value = current.copy(
                errorMessage = "Unable to update sub-order status."
            )
            return
        }

        reload()
    }

    private suspend fun updateOrderStatusInternal(orderId: OrderId, status: OrderStatus) {
        val current = _uiState.value
        _uiState.value = current.copy(errorMessage = null)

        val success = try {
            orderAdminService.updateOrderStatus(orderId, status)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            _uiState.value = current.copy(
                errorMessage = throwable.message ?: "Unable to update order status."
            )
            return
        }

        if (!success) {
            _uiState.value = current.copy(
                errorMessage = "Unable to update order status."
            )
            return
        }

        reload()
    }

    private suspend fun reload() {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(errorMessage = null)

        val nextState = try {
            val merchants = orderAdminService.loadMerchantOptions().toPersistentList()
            val orders = orderAdminService.loadOrders(current.filter).toPersistentList()
            val selectedOrderId = current.selectedOrderId
                ?.takeIf { selectedId -> orders.any { order -> order.orderId == selectedId } }
                ?: orders.firstOrNull()?.orderId
            val selectedOrder = selectedOrderId?.let { orderAdminService.loadOrderDetailOrNull(it) }

            current.copy(
                errorMessage = null,
                merchants = merchants,
                orders = orders,
                selectedOrderId = selectedOrder?.order?.id ?: selectedOrderId,
                selectedOrder = selectedOrder
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                errorMessage = throwable.message ?: "Unable to load order operations."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    companion object {
        fun factory(orderAdminService: OrderAdminService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == OrderAdminViewModel::class) {
                        return OrderAdminViewModel(orderAdminService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }
}
