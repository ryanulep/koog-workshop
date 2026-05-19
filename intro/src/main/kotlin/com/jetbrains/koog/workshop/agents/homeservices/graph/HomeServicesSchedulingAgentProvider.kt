package com.jetbrains.koog.workshop.agents.homeservices.graph

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import com.jetbrains.koog.workshop.agents.util.AgentExecutionTraceEvent
import com.jetbrains.koog.workshop.agents.util.CommunicationTools
import com.jetbrains.koog.workshop.agents.util.TaskAgentProvider
import com.jetbrains.koog.workshop.agents.util.trackEvents
import koog_workshop.intro.generated.resources.Res
import koog_workshop.intro.generated.resources.homeServicesAgent
import org.jetbrains.compose.resources.DrawableResource
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesBookingProvider
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesFindSlotTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesSchedule

internal class HomeServicesSchedulingAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : TaskAgentProvider {
    override val title: String = "Home Services Scheduling"
    override val description: String =
        "Hi! I'm the home services scheduling assistant. I can gather the details and book a service visit for you."
    override val avatarRes: DrawableResource = Res.drawable.homeServicesAgent

    override suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AIAgent<String, String> {
        val (llmClient, model) = provideLLMClient.invoke()
        val executor = MultiLLMPromptExecutor(llmClient)
        val communicationTools = CommunicationTools(onAssistantMessage).asTools()
        val schedule = HomeServicesSchedule()
        val findTools = HomeServicesFindSlotTools(schedule).asTools()
        val bookingProvider = HomeServicesBookingProvider(schedule)

        val agentConfig = AIAgentConfig(
            prompt = prompt("home-services-scheduling") {
                system(HomeServicesPrompts.systemPrompt())
            },
            model = model,
            maxAgentIterations = 200
        )

        return AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = homeServicesStrategy(communicationTools, findTools, bookingProvider),
            toolRegistry = ToolRegistry {
                tools(communicationTools)
                tools(findTools)
            },
        ) {
            install(EventHandler) {
                trackEvents(onToolCallEvent, onErrorEvent, onLLMCallEvent, onExecutionTraceEvent)
            }
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
            }
        }
    }
}