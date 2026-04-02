package org.example.project

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.admin.app.AdminRoute
import org.example.project.admin.data.createAdminDatabase
import org.example.project.admin.data.createDataSource
import org.example.project.domain.admin.MerchantAdminService
import org.example.project.domain.admin.OrderAdminService
import org.example.project.domain.admin.ProductAdminService
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

val AdminColorScheme: ColorScheme = lightColorScheme(
    // Primary: Deep enchanted purple (magic, mystique)
    primary = Color(0xFF6B21A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9D5FF),
    onPrimaryContainer = Color(0xFF3F0F7B),

    // Secondary: Deep ocean blue (wisdom, knowledge)
    secondary = Color(0xFF0C4A6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE2F3),
    onSecondaryContainer = Color(0xFF001D3D),

    // Tertiary: Glowing golden amber (treasure, magic)
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDD3),
    onTertiaryContainer = Color(0xFF5A2E0D),

    // Background & Surface: Rich dark cream (parchment, aged)
    background = Color(0xFFFAF5F0),
    onBackground = Color(0xFF1A1410),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF1A1410),

    // Surface variant: Soft mauve tone for cards/sections
    surfaceVariant = Color(0xFFE8D9EF),
    onSurfaceVariant = Color(0xFF3E2C47),

    // Error: Bold crimson (warnings, danger)
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF6B1C1C)
)

@Composable
fun AdminApp(database: Database) {
    val productAdminService = remember(database) { ProductAdminService(database) }
    val merchantAdminService = remember(database) { MerchantAdminService(database) }
    val orderAdminService = remember(database) { OrderAdminService(database) }

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
