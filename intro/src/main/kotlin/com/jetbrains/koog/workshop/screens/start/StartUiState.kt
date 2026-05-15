package com.jetbrains.koog.workshop.screens.start

import com.jetbrains.koog.workshop.NavRoute
import koog_workshop.intro.generated.resources.Res
import koog_workshop.intro.generated.resources.homeServicesAgent
import koog_workshop.intro.generated.resources.weatherAgent
import org.jetbrains.compose.resources.DrawableResource

data class StartUiState(
    val isApiKeyConfigured: Boolean = false,
    val demoCards: List<CardItem> = listOf(
        CardItem(
            title = "Weather Forecast",
            description = "Get forecasts, conditions, and weather insights for any location.",
            imageRes = Res.drawable.weatherAgent,
            agentDemoRoute = NavRoute.AgentDemoRoute.WeatherScreen
        ),
        CardItem(
            title = "Home Services",
            description = "Schedule plumbing, electrical, HVAC, or handyman service.",
            imageRes = Res.drawable.homeServicesAgent,
            agentDemoRoute = NavRoute.AgentDemoRoute.HomeServicesScreen
        ),
    )
)

data class CardItem(
    val title: String,
    val description: String,
    val imageRes: DrawableResource? = null,
    val agentDemoRoute: NavRoute.AgentDemoRoute? = null,
)
