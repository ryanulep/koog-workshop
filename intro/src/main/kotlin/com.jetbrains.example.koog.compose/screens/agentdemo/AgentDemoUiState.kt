package com.jetbrains.example.koog.compose.screens.agentdemo

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message

// Define UI state for the agent demo screen
data class AgentDemoUiState(
    val title: String = "Agent Demo",
    val chatMessages: List<ChatMessage> = listOf(ChatMessage.SystemMessage("Hi, I'm an agent that can help you")),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
)

// Define message types for the chat
sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class AgentMessage(val text: String) : ChatMessage()
    data class SystemMessage(val text: String) : ChatMessage()
    data class ErrorMessage(val text: String) : ChatMessage()
    data class ToolCallMessage(val toolName: String, val args: Map<String, String>) : ChatMessage()
    data class LLMCallMessage(val data: LlmCallData) : ChatMessage()
    data class ResultMessage(val text: String) : ChatMessage()
}

data class LlmCallData(
    val messageHistory: List<LlmCallHistoryItem>,
    val availableTools: List<LlmCallToolData>,
)

sealed interface LlmCallHistoryItem {
    val text: String

    data class System(override val text: String) : LlmCallHistoryItem
    data class User(override val text: String) : LlmCallHistoryItem
    data class Assistant(override val text: String) : LlmCallHistoryItem
    data class Reasoning(override val text: String) : LlmCallHistoryItem
    data class ToolCall(val toolName: String, override val text: String) : LlmCallHistoryItem
    data class ToolResult(val toolName: String, override val text: String) : LlmCallHistoryItem
}

data class LlmCallToolData(
    val name: String,
    val requiredParameters: List<String>,
    val optionalParameters: List<String>,
)

fun List<Message>.toHistoryItems(): List<LlmCallHistoryItem> =
    map { message ->
        when (message) {
            is Message.System -> LlmCallHistoryItem.System(message.content)
            is Message.User -> LlmCallHistoryItem.User(message.content)
            is Message.Assistant -> LlmCallHistoryItem.Assistant(message.content)
            is Message.Reasoning -> LlmCallHistoryItem.Reasoning(message.content)
            is Message.Tool.Call -> LlmCallHistoryItem.ToolCall(message.tool, message.content)
            is Message.Tool.Result -> LlmCallHistoryItem.ToolResult(message.tool, message.content)
        }
    }

fun List<ToolDescriptor>.toToolData(): List<LlmCallToolData> =
    map { tool ->
        LlmCallToolData(
            name = tool.name,
            requiredParameters = tool.requiredParameters.map { it.name },
            optionalParameters = tool.optionalParameters.map { it.name }
        )
    }
