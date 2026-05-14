package org.example.project.admin.dashboard

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class AdminDashboardService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    suspend fun loadRecentOrders(limit: Int = 5): List<RecentOrderSummary> =
        httpClient.get("$baseUrl/admin/dashboard/recent-orders") {
            parameter("limit", limit)
        }.body()

    suspend fun loadOrderHistory(): List<RecentOrderSummary> =
        httpClient.get("$baseUrl/admin/dashboard/order-history").body()
}
