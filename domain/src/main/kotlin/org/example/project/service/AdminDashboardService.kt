package org.example.project.service

import org.example.project.db.repository.AdminDashboardRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.model.DashboardSnapshot
import org.jetbrains.exposed.v1.jdbc.Database

class AdminDashboardService(
    private val database: Database,
    private val dashboardRepository: AdminDashboardRepository = AdminDashboardRepository()
) {
    suspend fun loadDashboard(): DashboardSnapshot =
        database.suspendTransaction {
            dashboardRepository.getDashboardSnapshot()
        }
}
