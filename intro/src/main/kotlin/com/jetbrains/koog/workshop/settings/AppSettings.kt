package com.jetbrains.koog.workshop.settings

import kotlinx.coroutines.flow.StateFlow

interface AppSettings {
    val appearanceModeFlow: StateFlow<AppearanceMode>
    suspend fun getCurrentSettings(): AppSettingsData
    suspend fun setCurrentSettings(settings: AppSettingsData)
    suspend fun updateAppearanceMode(mode: AppearanceMode)
}

// Data stored in the settings
data class AppSettingsData(
    val openAiToken: String,
    val anthropicToken: String,
    val geminiToken: String,
    val selectedOption: SelectedOption,
    val appearanceMode: AppearanceMode = AppearanceMode.Auto,
)

sealed class SelectedOption(val title: String) {
    data object OpenAI : SelectedOption("OpenAI")
    data object Anthropic : SelectedOption("Anthropic")
    data object Gemini : SelectedOption("Gemini")
}

enum class AppearanceMode(val label: String) {
    Auto("Auto"),
    Light("Light"),
    Dark("Dark"),
}
