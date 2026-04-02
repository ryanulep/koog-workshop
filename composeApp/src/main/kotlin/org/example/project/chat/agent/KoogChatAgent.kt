package org.example.project.chat.agent

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgent.Companion.invoke
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import org.example.project.domain.chat.ChatAgent
import java.util.UUID

class KoogChatAgent(
    private val sessionId: String = UUID.randomUUID().toString(),
    private val executor: PromptExecutor =
) : ChatAgent {
    override suspend fun sendMessage(userMessage: String): String {
        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = "You are a helpful Fantasy Store assistant. Help customers with products, orders, and general inquiries.",
            llmModel = OpenAIModels.Chat.GPT4oMini,
            temperature = 0.7,
            maxIterations = 10
        ) {
            install(ChatMemory) { windowSize(50) }
        }

        return agent.run(userMessage, sessionId)
    }
}
