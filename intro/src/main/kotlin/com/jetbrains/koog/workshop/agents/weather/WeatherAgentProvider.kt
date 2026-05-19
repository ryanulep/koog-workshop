package com.jetbrains.koog.workshop.agents.weather

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
import com.jetbrains.koog.workshop.agents.util.ChatAgentProvider
import com.jetbrains.koog.workshop.agents.util.trackEvents
import com.jetbrains.koog.workshop.agents.weather.WeatherPrompts.systemPrompt
import koog_workshop.intro.generated.resources.Res
import koog_workshop.intro.generated.resources.weatherAgent
import org.jetbrains.compose.resources.DrawableResource
import kotlin.time.ExperimentalTime

/**
 * Factory for creating weather forecast agents
 */
@OptIn(ExperimentalTime::class)
internal class WeatherAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : ChatAgentProvider {
    override val title: String = "Weather Forecast"
    override val description: String = "Hi, I'm a weather agent. I can provide weather forecasts for any location."
    override val avatarRes: DrawableResource = Res.drawable.weatherAgent

    override suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
    ): AIAgent<String, String> {
        val (llmClient, model) = provideLLMClient.invoke()
        val executor = MultiLLMPromptExecutor(llmClient)

        val agentConfig = AIAgentConfig(
            prompt = prompt("test") {
                system(systemPrompt)
            },
            model = model,
            maxAgentIterations = 50
        )

        return AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            // TODO: Add weather tools
        ) {
            install(EventHandler) {
                trackEvents(onToolCallEvent, onErrorEvent, onLLMCallEvent, onExecutionTraceEvent)
            }
        }
    }
}
