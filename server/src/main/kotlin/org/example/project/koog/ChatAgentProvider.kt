package org.example.project.koog

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.example.project.domain.order.OrderService
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.tools.AskQuestionTool
import org.example.project.koog.tools.CustomerSupportTools
import org.example.project.koog.tools.ReadOrderTools
import org.example.project.koog.tools.UpdateOrderTools
import org.example.project.koog.tracking.AgentExecutionTraceEvent
import org.example.project.koog.tracking.trackEvents
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ChatAgentProvider(
    private val executor: PromptExecutor,
    private val orderService: OrderService,
    @Value($$"${langfuse.secretkey}") private val langfuseSecretKey: String,
    @Value($$"${langfuse.publickey}") private val langfusePublicKey: String,
    @Value($$"${langfuse.url}") private val langfuseUrl: String,
) {
    fun provideAgent(
        characterId: CharacterId,
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: (String) -> Unit,
        onExecutionTraceEvent: (AgentExecutionTraceEvent) -> Unit,
        onAskMessage: (String) -> Unit,
    ): AIAgent<String, String> {
        val askQuestionTool = AskQuestionTool(onAskMessage)
        val readOrderTools = ReadOrderTools(characterId, orderService)
        val updateOrderTools = UpdateOrderTools(characterId, orderService)
        val tools = CustomerSupportTools(askQuestionTool, readOrderTools, updateOrderTools)

        return AIAgent(
            promptExecutor = executor,
//            strategy = orderCustomerSupportStrategy(tools),
            systemPrompt = """
                | You are a helpful Fantasy Store assistant. Help customers with products, orders, and general inquiries.
                | Use the askQuestion in case you're unsure or there is any missing data for solve the issue.
            """.trimMargin(),
            llmModel = OpenAIModels.Chat.GPT5_4,
            toolRegistry = ToolRegistry.Companion {
                tools(askQuestionTool)
                tools(readOrderTools)
//                tools(updateOrderTools)
            },
        ) {
//            install(ChatMemory) {
//                chatHistoryProvider = historyProvider
//                windowSize(50)
//            }
            install(OpenTelemetry) {
                setServiceInfo("customer-support", "0.0.1")
                addLangfuseExporter(langfuseUrl, langfusePublicKey, langfuseSecretKey)
            }
            install(Tracing) {
                addMessageProcessor(TraceFeatureMessageLogWriter(KotlinLogging.logger {}))
            }
            trackEvents(onToolCallEvent, onErrorEvent, onLLMCallEvent, onExecutionTraceEvent)
        }
    }
}