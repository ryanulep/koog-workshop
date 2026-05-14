package com.jetbrains.koog.workshop.agents.homeservices.basic

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import com.jetbrains.koog.workshop.agents.util.AgentExecutionTraceEvent
import com.jetbrains.koog.workshop.agents.util.ChatAgentProvider
import com.jetbrains.koog.workshop.agents.util.trackEvents
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesBookTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesFindSlotTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesSchedule

internal class HomeServicesBasicAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : ChatAgentProvider {
    override val title: String = "Home Services Scheduling (Basic)"
    override val description: String =
        "Hi! I'm the home services scheduling assistant. I can gather the details and book a service visit for you."

    override suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
    ): AIAgent<String, String> {
        val (llmClient, model) = provideLLMClient.invoke()
        val executor = MultiLLMPromptExecutor(llmClient)
        val schedule = HomeServicesSchedule()
        val findTools = HomeServicesFindSlotTools(schedule)
        val bookTools = HomeServicesBookTools(schedule)

        val agentConfig = AIAgentConfig(
            prompt = prompt("home-services-basic") {
                system(homeServicesBasicSystemPrompt())
            },
            model = model,
            maxAgentIterations = 200
        )

        return AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            toolRegistry = ToolRegistry {
                tools(findTools)
                tools(bookTools)
            },
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
            }
            trackEvents(onToolCallEvent, onErrorEvent, onLLMCallEvent, onExecutionTraceEvent)
        }
    }
}
