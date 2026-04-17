package com.jetbrains.example.koog.compose.agents.weather

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import com.jetbrains.example.koog.compose.agents.common.AgentProvider
import kotlin.time.ExperimentalTime

/**
 * Factory for creating weather forecast agents
 */
@OptIn(ExperimentalTime::class)
internal class WeatherAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : AgentProvider {
    override val title: String = "Weather Forecast"
    override val description: String = "Hi, I'm a weather agent. I can provide weather forecasts for any location."

    override suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
    ): AIAgent<String, String> {
        val (llmClient, model) = provideLLMClient.invoke()
        val executor = MultiLLMPromptExecutor(llmClient)

        val toolRegistry = ToolRegistry {
            tool(WeatherTools.CurrentDatetimeTool())
            tool(WeatherTools.AddDatetimeTool())
            tool(WeatherTools.WeatherForecastTool())
        }

        val strategy = strategy(title) {
            val nodeRequestLLM by nodeLLMRequestMultiple()
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            edge(nodeStart forwardTo nodeRequestLLM)

            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple
                    onMultipleToolCalls { true }
            )

            edge(
                nodeRequestLLM forwardTo nodeFinish
                    transformed { it.first() }
                    onAssistantMessage { true }
            )

            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory)
                    onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
            )

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                    onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
            )

            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                    onMultipleToolCalls { true }
            )

            edge(
                nodeSendToolResultMultiple forwardTo nodeFinish
                    transformed { it.first() }
                    onAssistantMessage { true }
            )
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("test") {
                system(
                    """
                    You are a helpful weather assistant.
                    You can provide weather forecasts for any location in the world and help the user plan their activities.

                    Use the tools at your disposal to:
                    1. Get the current date and time
                    2. Add days, hours, or minutes to a date
                    3. Get weather forecasts for specific locations and dates

                    ALWAYS USE current_datetime and add_datetime tools to perform date operations, do not try to guess.

                    When providing weather forecasts, be helpful and informative, explaining the weather conditions in a clear way.
                    """.trimIndent()
                )
            },
            model = model,
            maxAgentIterations = 50
        )

        return AIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry,
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
                windowSize(50)
            }
            handleEvents {
                onToolCallStarting { ctx ->
                    onToolCallEvent("Tool ${ctx.toolName}, args ${ctx.toolArgs}")
                }

                onAgentExecutionFailed { ctx ->
                    onErrorEvent("${ctx.throwable.message}")
                }
            }
        }
    }
}
