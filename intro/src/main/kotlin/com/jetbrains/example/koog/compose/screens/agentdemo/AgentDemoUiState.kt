package com.jetbrains.example.koog.compose.screens.agentdemo

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message

// Define UI state for the agent demo screen
data class AgentDemoUiState(
    val title: String = "Agent Demo",
    val chatMessages: List<ChatMessage> = listOf(ChatMessage.SystemMessage("Hi, I'm an agent that can help you")),
    val debugView: DebugView = DebugView(),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isChatEnded: Boolean = false,
    val userResponseRequested: Boolean = false,
    val currentUserResponse: String? = null,
)

enum class DebugOption(val title: String) {
    Tools("Tools"),
    LlmCalls("LLM Calls"),
    Nodes("Nodes"),
    Tasks("Tasks"),
}

data class DebugView(
    val enabled: Boolean = false,
    val options: Set<DebugOption> = DebugOption.entries.toSet(),
) {
    fun shows(message: ChatMessage): Boolean = shows(message.type)

    fun shows(type: ChatMessageType): Boolean {
        if (type in alwaysVisible) return true
        if (!enabled) return false
        return when (type) {
            ChatMessageType.Error -> true
            ChatMessageType.ToolCall -> DebugOption.Tools in options
            ChatMessageType.LlmCall -> DebugOption.LlmCalls in options
            ChatMessageType.Node -> DebugOption.Nodes in options
            ChatMessageType.Task -> DebugOption.Tasks in options
            else -> true
        }
    }

    companion object {
        private val alwaysVisible = setOf(
            ChatMessageType.User,
            ChatMessageType.Agent,
            ChatMessageType.System,
            ChatMessageType.Result,
        )
    }
}

enum class ChatMessageType {
    User,
    Agent,
    System,
    Error,
    Result,
    ToolCall,
    LlmCall,
    Node,
    Task,
}

// Define message types for the chat
sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class AgentMessage(val text: String) : ChatMessage()
    data class SystemMessage(val text: String) : ChatMessage()
    data class ErrorMessage(val text: String) : ChatMessage()
    data class ResultMessage(val text: String) : ChatMessage()
    data class ToolCallMessage(val toolName: String, val args: Map<String, String>) : ChatMessage()
    data class LLMCallMessage(val data: LlmCallData) : ChatMessage()
    data class ExecutionTraceMessage(val item: ExecutionTraceItem) : ChatMessage()
}

val ChatMessage.type: ChatMessageType
    get() =
        when (this) {
            is ChatMessage.UserMessage -> ChatMessageType.User
            is ChatMessage.AgentMessage -> ChatMessageType.Agent
            is ChatMessage.SystemMessage -> ChatMessageType.System
            is ChatMessage.ErrorMessage -> ChatMessageType.Error
            is ChatMessage.ResultMessage -> ChatMessageType.Result
            is ChatMessage.ToolCallMessage -> ChatMessageType.ToolCall
            is ChatMessage.LLMCallMessage -> ChatMessageType.LlmCall
            is ChatMessage.ExecutionTraceMessage -> when (item) {
                is ExecutionTraceItem.Node -> ChatMessageType.Node
                is ExecutionTraceItem.Subgraph -> ChatMessageType.Task
            }
        }

sealed interface ExecutionTraceItem {
    val name: String

    data class Node(override val name: String) : ExecutionTraceItem
    data class Subgraph(override val name: String) : ExecutionTraceItem
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
