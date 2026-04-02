package org.example.project.admin.orders.operations

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.example.project.domain.admin.AdminOrderDetail
import org.example.project.domain.admin.OrderFilter
import org.example.project.domain.admin.OrderListItem
import org.example.project.domain.admin.OrderMerchantOption
import org.example.project.domain.shared.OrderId

@Immutable
data class OrderAdminUiState(
    val errorMessage: String? = null,
    val filter: org.example.project.domain.admin.OrderFilter = _root_ide_package_.org.example.project.domain.admin.OrderFilter(),
    val merchants: PersistentList<org.example.project.domain.admin.OrderMerchantOption> = persistentListOf(),
    val orders: PersistentList<org.example.project.domain.admin.OrderListItem> = persistentListOf(),
    val selectedOrderId: org.example.project.domain.shared.OrderId? = null,
    val selectedOrder: org.example.project.domain.admin.AdminOrderDetail? = null
)
