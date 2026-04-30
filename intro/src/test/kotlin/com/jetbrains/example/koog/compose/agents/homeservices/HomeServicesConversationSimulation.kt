package com.jetbrains.example.koog.compose.agents.homeservices

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.jetbrains.example.koog.compose.agents.common.AskUserTool
import dev.dokimos.core.JudgeLM
import dev.dokimos.core.conversation.ConversationTrajectory
import dev.dokimos.core.conversation.LLMSimulatedUser
import dev.dokimos.core.conversation.Message as DokimosMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.test.Test

class HomeServicesConversationSimulation {

    @Test
    fun `simulate happy path plumbing conversation`() {
        val apiKey = System.getenv("OPENAI_API_KEY") ?: run {
            println("OPENAI_API_KEY not set, skipping simulation")
            return
        }

        // 1. Reusable executor for the judge LM (avoid creating a new one per call)
        val judgeExecutor = simpleOpenAIExecutor(apiKey)
        val judge = JudgeLM { prompt ->
            runBlocking {
                AIAgent(
                    promptExecutor = judgeExecutor,
                    llmModel = OpenAIModels.Chat.GPT4o,
                    maxIterations = 30
                ).run(prompt)
            }
        }

        // 2. Simulated user with persona and behavior guidelines
        val simulatedUser = LLMSimulatedUser.builder()
            .judge(judge)
            .persona("homeowner with a leaking kitchen faucet")
            .behaviorGuidelines("""
                - Answer one question at a time, clearly and concisely
                - Your address is 42 Maple Street
                - You prefer morning appointments
                - Your name is Alex Johnson
                - You have no special access notes
                - Confirm booking on first ask
                - Give rating 5 when asked
            """.trimIndent())
            .build()

        // 3. Conversation tracking
        val conversation = mutableListOf<Pair<String, String>>()

        val initialMessage = "I have a leaking faucet in my kitchen."
        conversation.add("User" to initialMessage)

        // 4. Build the home services agent with simulated user as the responder
        val schedule = HomeServicesSchedule()
        val findTools = HomeServicesFindTools(schedule)
        val bookTools = HomeServicesBookTools(schedule)
        val askUserTool = AskUserTool { question ->
            conversation.add("Assistant" to question)
            System.out.println("[Assistant] $question")
            System.out.flush()

            val trajectory = ConversationTrajectory(
                conversation.map { (role, content) ->
                    if (role == "User") DokimosMessage.user(content) else DokimosMessage.assistant(content)
                },
                "Schedule plumbing service for a kitchen faucet leak",
                emptyMap<String, Any>()
            )

            val response = withContext(Dispatchers.IO) {
                simulatedUser.generateMessage(trajectory)
            }
            val responseText = response.content()

            conversation.add("User" to responseText)
            System.out.println("[User] $responseText")
            System.out.flush()

            responseText
        }

        val agent = AIAgent(
            promptExecutor = MultiLLMPromptExecutor(OpenAILLMClient(apiKey)),
            agentConfig = AIAgentConfig(
                prompt = prompt("home-services-scheduling") {
                    system(homeServicesSystemPrompt())
                },
                model = OpenAIModels.Chat.GPT4o,
                maxAgentIterations = 200
            ),
            strategy = homeServicesSchedulingStrategy(askUserTool, findTools, bookTools),
            toolRegistry = ToolRegistry {
                tools(askUserTool)
                tools(findTools)
                tools(bookTools)
            }
        )

        // 5. Run the conversation
        runBlocking {
            agent.run(initialMessage)
        }

        // 6. Write results to file
        val outputFile = File("build/conversation-simulation.txt")
        outputFile.parentFile.mkdirs()
        outputFile.bufferedWriter().use { writer ->
            for ((role, content) in conversation) {
                writer.write("$role: $content")
                writer.newLine()
                writer.newLine()
            }
        }
        println("Conversation written to ${outputFile.absolutePath}")
    }
}
