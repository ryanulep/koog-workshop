package org.example.project

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.admin.AdminRoute
import org.example.project.admin.createAdminDatabase
import org.jetbrains.exposed.v1.jdbc.Database
import org.example.project.domain.admin.OrderAdminService
import org.example.project.domain.admin.ProductAdminService

private val AdminColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDE68A),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun App(database: Database) {
    val productAdminService = remember(database) { ProductAdminService(database) }
    val orderAdminService = remember(database) { OrderAdminService(database) }

    MaterialTheme(colorScheme = AdminColorScheme) {
        AdminRoute(
            productAdminService = productAdminService,
            orderAdminService = orderAdminService
        )
    }
}

@Composable
@Preview
fun App() {
    val database = remember { createAdminDatabase() }
    App(database = database)
}
