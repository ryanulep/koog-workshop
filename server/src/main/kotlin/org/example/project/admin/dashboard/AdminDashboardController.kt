package org.example.project.admin.dashboard

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService
) {
    @GetMapping("/recent-orders")
    fun getRecentOrders(@RequestParam(defaultValue = "5") limit: Int): List<RecentOrderSummary> {
        return adminDashboardService.loadRecentOrders(limit)
    }

    @GetMapping("/order-history")
    fun getOrderHistory(): List<RecentOrderSummary> {
        return adminDashboardService.loadOrderHistory()
    }
}
