package org.example.project.koog

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
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

class ChatAgentProvider(
    private val executor: PromptExecutor,
    private val orderService: OrderService,
) {
    fun provideAgent(
        characterId: CharacterId,
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AIAgent<String, String> {
        val askQuestionTool = AskQuestionTool(onAssistantMessage)
        val readOrderTools = ReadOrderTools(characterId, orderService)
        val updateOrderTools = UpdateOrderTools(characterId, orderService)
        val tools = CustomerSupportTools(askQuestionTool, readOrderTools, updateOrderTools)

        return AIAgent.Companion(
            promptExecutor = executor,
            strategy = orderCustomerSupportStrategy(tools),
            systemPrompt = """
                | You are a helpful Fantasy Store assistant. Help customers with products, orders, and general inquiries.
                | Use the askQuestion in case you're unsure or there is any missing data for solve the issue.
            """.trimMargin(),
            llmModel = OpenAIModels.Chat.GPT5_4,
            toolRegistry = ToolRegistry.Companion {
                tools(askQuestionTool)
                tools(readOrderTools)
                tools(updateOrderTools)
            },
        ) {
            install(ChatMemory.Feature) {
                chatHistoryProvider = historyProvider
                windowSize(50)
            }
            install(Tracing.Feature) {
                addMessageProcessor(TraceFeatureMessageLogWriter(KotlinLogging.logger {}))
            }
            trackEvents(onToolCallEvent, onErrorEvent, onLLMCallEvent, onExecutionTraceEvent)
        }
    }
}