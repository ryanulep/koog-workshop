package com.jetbrains.example.koog.compose.agents.util

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message

sealed interface AgentProvider {
    val title: String
    val description: String
}

sealed interface AgentExecutionTraceEvent {
    val name: String

    data class Node(override val name: String) : AgentExecutionTraceEvent
    data class Subgraph(override val name: String) : AgentExecutionTraceEvent
}

interface ChatAgentProvider : AgentProvider {
    suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
    ): AIAgent<String, String>
}

interface TaskAgentProvider : AgentProvider {
    suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AIAgent<String, String>
}
