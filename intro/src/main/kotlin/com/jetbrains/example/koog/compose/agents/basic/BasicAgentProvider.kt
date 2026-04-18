package com.jetbrains.example.koog.compose.agents.basic

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import com.jetbrains.example.koog.compose.agents.common.AgentProvider
import com.jetbrains.example.koog.compose.agents.common.trackSystemMessages

/**
 * Factory for creating basic chat agents
 */
internal class BasicAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : AgentProvider {
    override val title: String = "Basic Chat"
    override val description: String = "Hi, I'm a basic chat agent. I can chat with you about anything."

    override suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,        onErrorEvent: suspend (String) -> Unit,
    ): AIAgent<String, String> {
        val (llmClient, model) = provideLLMClient.invoke()
        val executor = MultiLLMPromptExecutor(llmClient)

        val agentConfig = AIAgentConfig(
            prompt = prompt("test") {
                system("You are a helpful assistant. You can chat with the user about anything.")
            },
            model = model,
            maxAgentIterations = 50
        )

        return AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
                windowSize(50)
            }
            trackSystemMessages(onToolCallEvent, onErrorEvent, onLLMCallEvent)
        }
    }
}
