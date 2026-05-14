package org.example.project.admin.dashboard

import kotlin.time.Instant
import org.example.project.admin.dashboard.RecentOrderSummary
import org.example.project.domain.character.Characters
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.Orders
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.OrderId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Service

@Service
class AdminDashboardRepository {
    private val recentOrdersJoin = Orders
        .innerJoin(Characters)
        .innerJoin(Currencies)

    fun getRecentOrders(limit: Int = 5): List<RecentOrderSummary> =
        getOrderSummaries(limit)

    fun getOrderSummaries(limit: Int? = null): List<RecentOrderSummary> {
        if (limit != null && limit <= 0) return emptyList()

        val query = recentOrdersJoin.selectAll()
            .orderBy(Orders.createdAt to SortOrder.DESC, Orders.id to SortOrder.DESC)

        val limitedQuery = limit?.let { query.limit(it) } ?: query

        return limitedQuery.map(::mapToRecentOrderSummary)
    }

    private fun mapToRecentOrderSummary(row: ResultRow) = RecentOrderSummary(
        orderId = OrderId(row[Orders.id].value),
        status = OrderStatus.valueOf(row[Orders.status]),
        characterName = row[Characters.name],
        totalPrice = row[Orders.totalPrice],
        totalCurrencyCode = row[Currencies.code],
        createdAt = row[Orders.createdAt]
    )
}
