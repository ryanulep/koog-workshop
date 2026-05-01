package com.jetbrains.example.koog.compose.agents.homeservices.graph

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
import com.jetbrains.example.koog.compose.agents.common.AgentExecutionTraceEvent
import com.jetbrains.example.koog.compose.agents.common.AskUserTool
import com.jetbrains.example.koog.compose.agents.common.TaskAgentProvider
import com.jetbrains.example.koog.compose.agents.common.trackSystemMessages
import com.jetbrains.example.koog.compose.agents.homeservices.HomeServicesBookTools
import com.jetbrains.example.koog.compose.agents.homeservices.HomeServicesFindTools
import com.jetbrains.example.koog.compose.agents.homeservices.HomeServicesSchedule

internal class HomeServicesSchedulingAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : TaskAgentProvider {
    override val title: String = "Home Services Scheduling (Graph)"
    override val description: String =
        "Hi! I'm the home services scheduling assistant (graph strategy). I can gather the details and book a service visit for you."

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
        val askUserTool = AskUserTool(onAssistantMessage)
        val schedule = HomeServicesSchedule()
        val findTools = HomeServicesFindTools(schedule)
        val bookTools = HomeServicesBookTools(schedule)

        val agentConfig = AIAgentConfig(
            prompt = prompt("home-services-scheduling") {
                system(homeServicesSystemPrompt())
            },
            model = model,
            maxAgentIterations = 200
        )

        return AIAgent.Companion(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = homeServicesSchedulingStrategy(askUserTool, findTools, bookTools),
            toolRegistry = ToolRegistry.Companion {
                tools(askUserTool)
                tools(findTools)
                tools(bookTools)
            },
        ) {
            install(ChatMemory.Feature) {
                chatHistoryProvider = historyProvider
            }
            trackSystemMessages(onToolCallEvent, onErrorEvent, onLLMCallEvent, onExecutionTraceEvent)
        }
    }
}