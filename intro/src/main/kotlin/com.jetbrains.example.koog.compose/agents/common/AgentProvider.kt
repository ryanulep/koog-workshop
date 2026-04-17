package com.jetbrains.example.koog.compose.agents.common

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent

/**
 * Interface for agent factory
 */
interface AgentProvider {
    /**
     * Title for the agent demo screen
     */
    val title: String

    /**
     * Description of the agent
     */
    val description: String

    suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
    ): AIAgent<String, String>
}
