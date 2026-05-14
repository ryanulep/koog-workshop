package org.example.project.shared

import kotlinx.serialization.Serializable

@Serializable
sealed class ChatMessage {
    @Serializable
    data class UserMessage(val text: String) : ChatMessage()

    @Serializable
    data class AskQuestion(val text: String) : ChatMessage()

    @Serializable
    data class AgentMessage(val text: String) : ChatMessage()

    @Serializable
    data class SystemMessage(val text: String) : ChatMessage()

    @Serializable
    data class ErrorMessage(val text: String) : ChatMessage()

    @Serializable
    data class ToolCallMessage(val toolName: String, val args: Map<String, String>) : ChatMessage()

    @Serializable
    data class LLMCallMessage(val data: LlmCallData) : ChatMessage()

    @Serializable
    data class ExecutionTraceMessage(val item: ExecutionTraceItem) : ChatMessage()
}

@Serializable
sealed interface ExecutionTraceItem {
    val name: String

    @Serializable
    data class Node(override val name: String) : ExecutionTraceItem

    @Serializable
    data class Subgraph(override val name: String) : ExecutionTraceItem
}

@Serializable
data class LlmCallData(
    val messageHistory: List<LlmCallHistoryItem>,
    val availableTools: List<LlmCallToolData>,
)

@Serializable
sealed interface LlmCallHistoryItem {
    val text: String

    @Serializable
    data class System(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class User(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class Assistant(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class Reasoning(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class ToolCall(val toolName: String, override val text: String) : LlmCallHistoryItem

    @Serializable
    data class ToolResult(val toolName: String, override val text: String) : LlmCallHistoryItem
}

@Serializable
data class LlmCallToolData(
    val name: String,
    val requiredParameters: List<String>,
    val optionalParameters: List<String>,
)
