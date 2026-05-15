package com.jetbrains.koog.workshop.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class DataStoreAppSettings(prefPathProvider: PrefPathProvider) : AppSettings {

    private val _appearanceModeFlow = MutableStateFlow(AppearanceMode.Auto)
    override val appearanceModeFlow: StateFlow<AppearanceMode> = _appearanceModeFlow.asStateFlow()

    override suspend fun updateAppearanceMode(mode: AppearanceMode) {
        _appearanceModeFlow.value = mode
        dataStore.edit { preferences ->
            preferences[APPEARANCE_MODE_KEY] = mode.label
        }
    }

    // Define keys for the preferences
    companion object {
        val OPENAI_TOKEN_KEY = stringPreferencesKey("openai_token")
        val ANTHROPIC_TOKEN_KEY = stringPreferencesKey("anthropic_token")
        val GEMINI_TOKEN_KEY = stringPreferencesKey("gemini_token")
        val SELECTED_PROVIDER_KEY = stringPreferencesKey("selected_provider")
        val APPEARANCE_MODE_KEY = stringPreferencesKey("appearance_mode")
    }

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { prefPathProvider.get() }
        )
    }

    override suspend fun getCurrentSettings(): AppSettingsData {
        val data = dataStore.data.map { preferences ->
            AppSettingsData(
                openAiToken = preferences[OPENAI_TOKEN_KEY].orEmpty(),
                anthropicToken = preferences[ANTHROPIC_TOKEN_KEY].orEmpty(),
                geminiToken = preferences[GEMINI_TOKEN_KEY].orEmpty(),
                selectedOption = when (preferences[SELECTED_PROVIDER_KEY]) {
                    SelectedOption.OpenAI.title -> SelectedOption.OpenAI
                    SelectedOption.Anthropic.title -> SelectedOption.Anthropic
                    SelectedOption.Gemini.title -> SelectedOption.Gemini
                    else -> SelectedOption.OpenAI
                },
                appearanceMode = when (preferences[APPEARANCE_MODE_KEY]) {
                    AppearanceMode.Light.label -> AppearanceMode.Light
                    AppearanceMode.Dark.label -> AppearanceMode.Dark
                    else -> AppearanceMode.Auto
                }
            )
        }.first()
        _appearanceModeFlow.value = data.appearanceMode
        return data
    }

    override suspend fun setCurrentSettings(settings: AppSettingsData) {
        _appearanceModeFlow.value = settings.appearanceMode
        dataStore.edit { preferences ->
            preferences[OPENAI_TOKEN_KEY] = settings.openAiToken
            preferences[ANTHROPIC_TOKEN_KEY] = settings.anthropicToken
            preferences[GEMINI_TOKEN_KEY] = settings.geminiToken
            preferences[SELECTED_PROVIDER_KEY] = settings.selectedOption.title
            preferences[APPEARANCE_MODE_KEY] = settings.appearanceMode.label
        }
    }
}
