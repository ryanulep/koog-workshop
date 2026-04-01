package org.example.project.admin

import org.example.project.domain.id.OrderItemId
import org.example.project.domain.model.AdminOrderDetail

sealed interface AdminOrderDetailUiState {
    data object Loading : AdminOrderDetailUiState

    data class Error(val message: String) : AdminOrderDetailUiState

    data class Ready(
        val detail: AdminOrderDetail,
        val selectedItemId: OrderItemId? = null
    ) : AdminOrderDetailUiState
}
