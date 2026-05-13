package org.example.project.admin.dashboard


import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class AdminDashboardService(
    private val dashboardRepository: AdminDashboardRepository = AdminDashboardRepository()
) {
    fun loadRecentOrders(limit: Int = 5): List<RecentOrderSummary> =
            dashboardRepository.getRecentOrders(limit)

    fun loadOrderHistory(): List<RecentOrderSummary> =
            dashboardRepository.getOrderSummaries(limit = null)
}
