package com.jetbrains.koog.workshop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.jetbrains.koog.workshop.screens.agentdemo.AgentDemoScreen
import com.jetbrains.koog.workshop.screens.agentdemo.AgentDemoViewModel
import com.jetbrains.koog.workshop.screens.settings.SettingsScreen
import com.jetbrains.koog.workshop.screens.settings.SettingsViewModel
import com.jetbrains.koog.workshop.screens.start.StartScreen
import com.jetbrains.koog.workshop.screens.start.StartViewModel
import com.jetbrains.koog.workshop.settings.AppSettings
import com.jetbrains.koog.workshop.theme.AppTheme
import kotlinx.serialization.Serializable
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

/**
 * Main navigation graph for the app
 */

@Composable
fun ComposeApp() {
    val koin = getKoin()
    val appSettings: AppSettings = remember { koin.get() }
    val appearanceMode by appSettings.appearanceModeFlow.collectAsState()
    val backStack = remember { mutableStateListOf<NavKey>(NavRoute.StartScreen) }

    LaunchedEffect(Unit) {
        appSettings.getCurrentSettings()
    }

    AppTheme(appearanceMode = appearanceMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val appNavigation = remember { AppNavigation(backStack = backStack) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<NavRoute.StartScreen> {
                        val vm = remember { koin.get<StartViewModel> { parametersOf(appNavigation) } }
                        StartScreen(viewModel = vm)
                    }

                    entry<NavRoute.SettingsScreen> {
                        val vm = remember { koin.get<SettingsViewModel> { parametersOf(appNavigation) } }
                        SettingsScreen(viewModel = vm)
                    }

                    entry<NavRoute.AgentDemoRoute.WeatherScreen> {
                        val vm = remember { koin.get<AgentDemoViewModel> { parametersOf(appNavigation, "weather") } }
                        AgentDemoScreen(viewModel = vm)
                    }

                    entry<NavRoute.AgentDemoRoute.HomeServicesScreen> {
                        val vm = remember { koin.get<AgentDemoViewModel> { parametersOf(appNavigation, "home-services") } }
                        AgentDemoScreen(viewModel = vm)
                    }
                }
            )
        }
    }
}

/**
 * Navigation routes for the app
 */
@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object StartScreen : NavRoute

    @Serializable
    data object SettingsScreen : NavRoute

    /**
     * Screens with agent demos
     */
    @Serializable
    sealed interface AgentDemoRoute : NavRoute {
        @Serializable
        data object WeatherScreen : AgentDemoRoute

        @Serializable
        data object HomeServicesScreen : AgentDemoRoute

        @Serializable
        data object HomeServicesBasicScreen : AgentDemoRoute
    }
}
