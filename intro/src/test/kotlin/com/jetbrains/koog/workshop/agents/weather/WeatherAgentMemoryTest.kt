package com.jetbrains.koog.workshop.agents.weather

import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import dev.dokimos.core.JudgeLM
import dev.dokimos.core.conversation.ConversationTrajectory
import dev.dokimos.core.conversation.EvaluationCriterion
import dev.dokimos.core.conversation.Message.assistant
import dev.dokimos.core.conversation.Message.user
import dev.dokimos.kotlin.core.EvalTestCase
import dev.dokimos.kotlin.dsl.conversation.trajectoryEvaluator
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue

class WeatherAgentMemoryTest {

    private lateinit var apiKey: String
    private lateinit var judge: JudgeLM

    @Before
    fun setup() {
        val key = System.getenv("OPENAI_API_KEY") ?: ""
        Assume.assumeTrue("OPENAI_API_KEY is not set", key.isNotEmpty())
        apiKey = key
        val executor = simpleOpenAIExecutor(key)
        judge = JudgeLM { prompt ->
            runBlocking {
                AIAgent(
                    promptExecutor = executor,
                    llmModel = OpenAIModels.Chat.GPT4o,
                    maxIterations = 30
                ).run(prompt)
            }
        }
    }

    @Test
    fun `agent remembers location from previous message`() {
        val provider = WeatherAgentProvider { OpenAILLMClient(apiKey) to OpenAIModels.Chat.GPT4o }
        val agent = runBlocking {
            provider.provideAgent(
                historyProvider = InMemoryChatHistoryProvider(),
                onToolCallEvent = { _, _ -> },
                onLLMCallEvent = { _, _ -> },
                onErrorEvent = { },
                onExecutionTraceEvent = { },
            )
        }
        val sessionId = "test-weather-memory"
        val conversation = mutableListOf<Pair<String, String>>()

        val message1 = "What's the weather in Munich today?"
        val response1 = runBlocking { agent.run(message1, sessionId) }
        conversation.add("User" to message1)
        conversation.add("Assistant" to response1)
        println("[User] $message1")
        println("[Assistant] $response1")

        val message2 = "What about tomorrow?"
        val response2 = runBlocking { agent.run(message2, sessionId) }
        conversation.add("User" to message2)
        conversation.add("Assistant" to response2)
        println("[User] $message2")
        println("[Assistant] $response2")

        val trajectory = ConversationTrajectory(
            conversation.map { (role, content) ->
                if (role == "User") user(content) else assistant(content)
            },
            "Weather Agent - Memory Test",
            emptyMap()
        )
        val locationRemembered = EvaluationCriterion(
            "Location Remembered",
            "The assistant provides a weather forecast for Munich in the second response, even though the user only said 'What about tomorrow?' without mentioning Munich again.",
            1.0
        )
        val result = trajectoryEvaluator(judge) {
            name = "Weather Memory - Location Context"
            threshold = 0.9
            criteria(listOf(locationRemembered))
        }.evaluate(EvalTestCase(actualOutputs = mapOf("trajectory" to trajectory)))
        println("score=${result.score()} passed=${result.success()} reason=${result.reason()}")
        assertTrue(result.success(), "Agent should remember Munich from the first message: ${result.reason()}")
    }
}
