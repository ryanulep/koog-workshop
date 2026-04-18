package com.jetbrains.example.koog.compose.agents.weather

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
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
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
    ): AIAgent<String, String> {
        val (llmClient, model) = provideLLMClient.invoke()
        val executor = MultiLLMPromptExecutor(llmClient)

        val toolRegistry = ToolRegistry {
            tools(WeatherTools())
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
            agentConfig = agentConfig,
//            strategy = singleRunStrategy(runMode = ToolCalls.SINGLE_RUN_SEQUENTIAL),
            toolRegistry = toolRegistry,
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
                windowSize(50)
            }
            handleEvents {
                onToolCallStarting { ctx ->
                    onToolCallEvent(ctx.toolName, ctx.toolArgs.entries.mapValues { it.value.toString() })
                }

                onAgentExecutionFailed { ctx ->
                    onErrorEvent("${ctx.throwable.message}")
                }

                onLLMCallStarting { ctx ->
                    onLLMCallEvent(ctx.prompt.messages, ctx.tools)
                }
            }
        }
    }
}
