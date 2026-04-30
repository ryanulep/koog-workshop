package com.jetbrains.example.koog.compose.agents.homeservices

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

internal class HomeServicesSchedulingAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : TaskAgentProvider {
    override val title: String = "Home Services Scheduling"
    override val description: String =
        "Hi! I'm the home services scheduling assistant. I can gather the details and book a service visit for you."

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

        return AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = homeServicesSchedulingStrategy(askUserTool, findTools, bookTools),
            toolRegistry = ToolRegistry {
                tools(askUserTool)
                tools(findTools)
                tools(bookTools)
            },
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
            }
            trackSystemMessages(onToolCallEvent, onErrorEvent, onLLMCallEvent, onExecutionTraceEvent)
        }
    }
}
