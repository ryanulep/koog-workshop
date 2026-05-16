package com.jetbrains.koog.workshop.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.koog.workshop.settings.ApiKeyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartViewModel(
    private val navigationCallback: StartNavigationCallback,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        refreshApiKeyStatus()
    }

    fun refreshApiKeyStatus() {
        val hasKey = try {
            ApiKeyService.apiKey
            true
        } catch (_: IllegalArgumentException) {
            false
        }
        _uiState.update { it.copy(isApiKeyConfigured = hasKey) }
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
