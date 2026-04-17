package com.jetbrains.example.koog.compose.screens.agentdemo

// Define UI state for the agent demo screen
data class AgentDemoUiState(
    val title: String = "Agent Demo",
    val messages: List<Message> = listOf(Message.SystemMessage("Hi, I'm an agent that can help you")),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
)

// Define message types for the chat
sealed class Message {
    data class UserMessage(val text: String) : Message()
    data class AgentMessage(val text: String) : Message()
    data class SystemMessage(val text: String) : Message()
    data class ErrorMessage(val text: String) : Message()
    data class ToolCallMessage(val text: String) : Message()
    data class ResultMessage(val text: String) : Message()
}
