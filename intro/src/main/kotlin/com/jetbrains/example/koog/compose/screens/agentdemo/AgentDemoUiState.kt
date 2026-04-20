package com.jetbrains.example.koog.compose.screens.agentdemo

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message

// Define UI state for the agent demo screen
data class AgentDemoUiState(
    val title: String = "Agent Demo",
    val chatMessages: List<ChatMessage> = listOf(ChatMessage.SystemMessage("Hi, I'm an agent that can help you")),
    val debugView: DebugView = DebugView.Off,
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
)

enum class DebugView(val title: String) {
    Off("Off"),
    Tools("Tools"),
    FullTrace("Full Trace");

    fun shows(message: ChatMessage): Boolean = shows(message.type)

    fun shows(type: ChatMessageType): Boolean =
        when (this) {
            Off -> type in setOf(ChatMessageType.User, ChatMessageType.Agent, ChatMessageType.System)
            Tools -> type != ChatMessageType.LlmCall
            FullTrace -> true
        }
}

enum class ChatMessageType {
    User,
    Agent,
    System,
    Error,
    ToolCall,
    LlmCall,
}

// Define message types for the chat
sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class AgentMessage(val text: String) : ChatMessage()
    data class SystemMessage(val text: String) : ChatMessage()
    data class ErrorMessage(val text: String) : ChatMessage()
    data class ToolCallMessage(val toolName: String, val args: Map<String, String>) : ChatMessage()
    data class LLMCallMessage(val data: LlmCallData) : ChatMessage()
}

val ChatMessage.type: ChatMessageType
    get() =
        when (this) {
            is ChatMessage.UserMessage -> ChatMessageType.User
            is ChatMessage.AgentMessage -> ChatMessageType.Agent
            is ChatMessage.SystemMessage -> ChatMessageType.System
            is ChatMessage.ErrorMessage -> ChatMessageType.Error
            is ChatMessage.ToolCallMessage -> ChatMessageType.ToolCall
            is ChatMessage.LLMCallMessage -> ChatMessageType.LlmCall
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
