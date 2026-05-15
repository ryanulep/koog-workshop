package com.jetbrains.koog.workshop.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.koog.workshop.settings.AppSettings
import com.jetbrains.koog.workshop.settings.SelectedOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartViewModel(
    private val appSettings: AppSettings,
    private val navigationCallback: StartNavigationCallback,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        refreshApiKeyStatus()
    }

    fun refreshApiKeyStatus() {
        viewModelScope.launch {
            val settings = appSettings.getCurrentSettings()
            val token = when (settings.selectedOption) {
                SelectedOption.OpenAI -> settings.openAiToken
                SelectedOption.Anthropic -> settings.anthropicToken
                SelectedOption.Gemini -> settings.geminiToken
            }
            _uiState.update { it.copy(isApiKeyConfigured = token.isNotBlank()) }
        }
    }

    fun onEvent(event: StartUiEvents) {
        viewModelScope.launch {
            when (event) {
                is StartUiEvents.AgentDemo -> navigationCallback.goAgentDemo(event.agentDemoRoute)
                StartUiEvents.Settings -> navigationCallback.goSettings()
            }
        }
    }
}
