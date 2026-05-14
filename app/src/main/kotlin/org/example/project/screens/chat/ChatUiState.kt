package org.example.project.screens.chat

import org.example.project.shared.ChatMessage
import org.example.project.shared.ExecutionTraceItem

data class ChatUiState(
    val title: String = "Chat",
    val chatMessages: List<ChatMessage> = emptyList(),
    val debugView: DebugView = DebugView(),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
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
        )
    }
}

enum class ChatMessageType {
    User,
    Agent,
    System,
    Error,
    ToolCall,
    LlmCall,
    Node,
    Task,
}


val ChatMessage.type: ChatMessageType
    get() =
        when (this) {
            is ChatMessage.UserMessage -> ChatMessageType.User
            is ChatMessage.AskQuestion -> ChatMessageType.Agent
            is ChatMessage.AgentMessage -> ChatMessageType.Agent
            is ChatMessage.SystemMessage -> ChatMessageType.System
            is ChatMessage.ErrorMessage -> ChatMessageType.Error
            is ChatMessage.ToolCallMessage -> ChatMessageType.ToolCall
            is ChatMessage.LLMCallMessage -> ChatMessageType.LlmCall
            is ChatMessage.ExecutionTraceMessage -> when (item) {
                is
                ExecutionTraceItem.Node -> ChatMessageType.Node
                is ExecutionTraceItem.Subgraph -> ChatMessageType.Task
            }
        }
