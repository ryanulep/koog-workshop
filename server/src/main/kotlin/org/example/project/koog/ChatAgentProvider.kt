package org.example.project.koog

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.persistence.jdbc.JdbcPersistenceStorageProvider
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import org.example.project.domain.order.OrderService
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.tools.CommunicationTools
import org.example.project.koog.tools.ReadOrderTools
import org.example.project.koog.tools.UpdateOrderTools
import org.example.project.koog.tracking.SseEmitterEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ChatAgentProvider(
    @Qualifier("multiLLMPromptExecutor")
    private val executor: MultiLLMPromptExecutor,
    private val orderService: OrderService,
    private val historyProvider: ChatHistoryProvider,
    private val persistence: JdbcPersistenceStorageProvider,
    @Value($$"${langfuse.secretkey}") private val langfuseSecretKey: String,
    @Value($$"${langfuse.publickey}") private val langfusePublicKey: String,
    @Value($$"${langfuse.url}") private val langfuseUrl: String,
) {
    fun provideAgent(
        characterId: CharacterId,
        sseEventHandler: SseEmitterEventHandler,
        communicationTools: CommunicationTools,
    ): AIAgent<String, String> {
        val readOrderTools = ReadOrderTools(characterId, orderService)
        val updateOrderTools = UpdateOrderTools(characterId, orderService)

        return AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                | You are a helpful Fantasy Store assistant. Help customers with products, orders, and general inquiries.
                | Use the askQuestion in case you're unsure or there is any missing data for solve the issue.
            """.trimMargin(),
            llmModel = OpenAIModels.Chat.GPT5_4,
            toolRegistry = ToolRegistry {
                tools(communicationTools)
                tools(readOrderTools)
                tools(updateOrderTools)
            },
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
                windowSize(50)
            }
            install(OpenTelemetry) {
                addLangfuseExporter(
                    langfuseSecretKey = langfuseSecretKey,
                    langfusePublicKey = langfusePublicKey,
                    langfuseUrl = langfuseUrl,
                )

                // See messages and node inputs and outputs
                setVerbose(true)
            }
            install(Persistence) {
                storage = persistence
            }
            install(Tracing) {
                addMessageProcessor(TraceFeatureMessageLogWriter(KotlinLogging.logger {}))
            }
            install(EventHandler, sseEventHandler.config)
        }
    }
}