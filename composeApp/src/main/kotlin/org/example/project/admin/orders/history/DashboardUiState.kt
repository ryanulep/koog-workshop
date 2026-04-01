package org.example.project.admin.orders.history

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import org.example.project.domain.admin.RecentOrderSummary

@Immutable
sealed interface DashboardUiState {
    data object Uninitialized : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Ready(val recentOrders: PersistentList<RecentOrderSummary>) : DashboardUiState
}
