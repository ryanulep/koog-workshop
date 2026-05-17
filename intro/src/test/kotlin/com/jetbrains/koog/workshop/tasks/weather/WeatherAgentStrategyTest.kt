package com.jetbrains.koog.workshop.tasks.weather

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import com.jetbrains.koog.workshop.agents.weather.WeatherPrompts
import com.jetbrains.koog.workshop.agents.weather.WeatherTools
import com.jetbrains.koog.workshop.agents.weather.basicSingleRunStrategyByHand
import com.jetbrains.koog.workshop.settings.ApiKeyService
import dev.dokimos.core.JudgeLM
import dev.dokimos.core.conversation.ConversationTrajectory
import dev.dokimos.core.conversation.EvaluationCriterion
import dev.dokimos.core.conversation.Message
import dev.dokimos.kotlin.core.EvalTestCase
import dev.dokimos.kotlin.dsl.conversation.trajectoryEvaluator
import kotlinx.coroutines.runBlocking
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WeatherAgentStrategyTest {
    private lateinit var llmClient: LLMClient
    private lateinit var model: LLModel
    private lateinit var judge: JudgeLM

    @Before
    fun setup() {
        val clientAndModel = ApiKeyService.getClientAndModel()
        llmClient = clientAndModel.first
        model = clientAndModel.second
        val executor = MultiLLMPromptExecutor(llmClient)
        judge = JudgeLM { prompt ->
            runBlocking {
                AIAgent(
                    promptExecutor = executor,
                    llmModel = model,
                    maxIterations = 30
                ).run(prompt)
            }
        }
    }

    @Test
    fun `agent with hand-written strategy provides weather forecast`() {
        val executor = MultiLLMPromptExecutor(llmClient)

        val toolRegistry = ToolRegistry {
            tools(WeatherTools())
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("test") {
                system(WeatherPrompts.systemPrompt)
            },
            model = model,
            maxAgentIterations = 50
        )

        val agent = AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = basicSingleRunStrategyByHand(),
            toolRegistry = toolRegistry,
        )

        val message = "What's the weather in Munich today?"
        val response = runBlocking { agent.run(message, "test-weather-strategy") }
        println("[User] $message")
        println("[Assistant] $response")

        val trajectory = ConversationTrajectory(
            listOf(Message.user(message), Message.assistant(response)),
            "Weather Agent - Strategy Test",
            emptyMap()
        )
        val forecastProvided = EvaluationCriterion(
            "Forecast Provided",
            "The assistant provides an actual weather forecast for Munich with specific details such as temperature or weather conditions. " +
                    "It does NOT say things like 'I need to check', 'Let me look that up', or 'I don't have access to weather data'. " +
                    "The response contains concrete weather information, not a promise to fetch it.",
            1.0
        )
        val result = trajectoryEvaluator(judge) {
            name = "Weather Strategy - Forecast Provided"
            threshold = 0.9
            criteria(listOf(forecastProvided))
        }.evaluate(EvalTestCase(actualOutputs = mapOf("trajectory" to trajectory)))
        println("score=${result.score()} passed=${result.success()} reason=${result.reason()}")
        assertTrue(result.success(), "Agent with hand-written strategy should provide an actual weather forecast: ${result.reason()}")
    }
}
