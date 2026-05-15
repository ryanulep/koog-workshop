package com.jetbrains.koog.workshop.screens.settings

import com.jetbrains.koog.workshop.settings.AppearanceMode
import com.jetbrains.koog.workshop.settings.SelectedOption

sealed interface SettingsUiEvents {
    data object NavigateBack : SettingsUiEvents
    data object SaveSettings : SettingsUiEvents
    data class UpdateOption(val selectedOption: SelectedOption) : SettingsUiEvents
    data class UpdateCredential(val credential: String) : SettingsUiEvents
    data class UpdateAppearance(val appearanceMode: AppearanceMode) : SettingsUiEvents
}
