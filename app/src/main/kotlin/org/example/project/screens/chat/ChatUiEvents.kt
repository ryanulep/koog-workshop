package org.example.project.screens.chat

sealed interface ChatUiEvents {
    data class UpdateInputText(val text: String) : ChatUiEvents
    data object ToggleDebugEnabled : ChatUiEvents
    data class ToggleDebugOption(val option: DebugOption) : ChatUiEvents
    data object SendMessage : ChatUiEvents
    data object RestartChat : ChatUiEvents
    data object NavigateBack : ChatUiEvents
}
