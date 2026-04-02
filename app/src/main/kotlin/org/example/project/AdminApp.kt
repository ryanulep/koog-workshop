package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.admin.app.AdminRoute
import org.example.project.admin.data.createAdminDatabase
import org.example.project.admin.data.createDataSource
import org.example.project.domain.admin.MerchantAdminService
import org.example.project.domain.admin.OrderAdminService
import org.example.project.domain.admin.ProductAdminService
import org.jetbrains.exposed.v1.jdbc.Database

@Composable
fun AdminApp(database: Database) {
    val productAdminService = remember(database) {
        _root_ide_package_.org.example.project.domain.admin.ProductAdminService(
            database
        )
    }
    val merchantAdminService = remember(database) {
        _root_ide_package_.org.example.project.domain.admin.MerchantAdminService(
            database
        )
    }
    val orderAdminService = remember(database) {
        _root_ide_package_.org.example.project.domain.admin.OrderAdminService(
            database
        )
    }

    AdminRoute(
        productAdminService = productAdminService,
        merchantAdminService = merchantAdminService,
        orderAdminService = orderAdminService
    )
}

@Composable
@Preview
fun AdminApp() {
    val database = remember { createAdminDatabase(createDataSource()) }
    AdminApp(database = database)
}
