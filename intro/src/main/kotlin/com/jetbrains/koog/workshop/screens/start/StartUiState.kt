package com.jetbrains.koog.workshop.screens.start

import com.jetbrains.koog.workshop.NavRoute

data class StartUiState(
    val demoCards: List<CardItem> = listOf(
        CardItem(
            title = "Weather Forecast",
            description = "A weather agent that can provide forecasts for any location. Ask about weather conditions, dates, and more.",
            agentDemoRoute = NavRoute.AgentDemoRoute.WeatherScreen
        ),
        CardItem(
            title = "Home Services",
            description = "Schedule plumbing, electrical, HVAC, or handyman service using a multi-phase graph strategy with gated tools.",
            agentDemoRoute = NavRoute.AgentDemoRoute.HomeServicesScreen
        ),
    )
)

data class CardItem(
    val title: String,
    val description: String,
    val agentDemoRoute: NavRoute.AgentDemoRoute? = null,
)
