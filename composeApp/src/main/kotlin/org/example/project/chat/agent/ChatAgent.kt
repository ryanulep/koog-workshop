package org.example.project.chat.agent

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.example.project.chat.ChatUi

class ChatAgent(
    private val sessionId: String,
    private val executor: PromptExecutor,
    val history: ChatHistoryProvider
) {
    suspend fun loadChat(conversationId: String): PersistentList<ChatUi.Message> {
        val history = history.load(conversationId)
        return history.mapNotNull { message ->
            when (message) {
                is Message.User -> ChatUi.Message.User(message.content)
                is Message.Assistant -> ChatUi.Message.CustomerSupport(message.content)
                is Message.Reasoning -> ChatUi.Message.CustomerSupport(message.content)
                is Message.System -> ChatUi.Message.CustomerSupport(message.content)
                is Message.Tool.Result,
                is Message.Tool.Call -> null
            }
        }.toPersistentList()
    }

    suspend fun sendMessage(userMessage: String): ChatUi.Message.CustomerSupport {
        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = "You are a helpful Fantasy Store assistant. Help customers with products, orders, and general inquiries.",
            llmModel = OpenAIModels.Chat.GPT5_4,
        ) {
            install(ChatMemory) {
                this.chatHistoryProvider = history
                windowSize(50)
            }
        }

        return ChatUi.Message.CustomerSupport(agent.run(userMessage, sessionId))
    }
}
