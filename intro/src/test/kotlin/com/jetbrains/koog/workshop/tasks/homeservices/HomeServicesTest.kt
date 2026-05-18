package com.jetbrains.koog.workshop.tasks.homeservices

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesBookTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesFindSlotTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesSchedule
import com.jetbrains.koog.workshop.agents.homeservices.graph.HomeServicesPrompts
import com.jetbrains.koog.workshop.agents.homeservices.graph.homeServicesStrategy
import com.jetbrains.koog.workshop.agents.util.CommunicationTools
import com.jetbrains.koog.workshop.settings.ApiKeyService
import dev.dokimos.core.JudgeLM
import dev.dokimos.core.conversation.ConversationTrajectory
import dev.dokimos.core.conversation.EvaluationCriterion
import dev.dokimos.core.conversation.LLMSimulatedUser
import dev.dokimos.kotlin.core.EvalTestCase
import dev.dokimos.kotlin.dsl.conversation.trajectoryEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue
import dev.dokimos.core.conversation.Message as DokimosMessage

class HomeServicesTest {
    private lateinit var llmClient: LLMClient
    private lateinit var model: LLModel
    private lateinit var judge: JudgeLM

    @Before
    fun setup() {
        val clientAndModel = ApiKeyService.getDefaultClientAndModel()
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
    fun `Scheduling - Plumbing leaking kitchen faucet`() = runCase(
        scenarioName = "Scheduling — Plumbing leaking kitchen faucet",
        initialMessage = "My kitchen faucet has a minor leak.",
        persona = "homeowner with a leaking kitchen faucet",
        behaviorGuidelines = """
            - Answer one question at a time, clearly and concisely
            - Your address is 42 Maple Street
            - You prefer morning appointments
            - Your name is Alex Johnson
            - You have no special access notes
            - Confirm booking on first ask
            - Give rating 5 when asked
        """.trimIndent(),
        criterion = APPOINTMENT_SCHEDULED,
    )

    @Test
    fun `Scheduling - Providing details up front - HVAC tune-up`() = runCase(
        scenarioName = "Providing details up front — HVAC tune-up",
        initialMessage = "Hi, I'm Jordan Lee at 88 Birchwood Drive. I need an HVAC tune-up before summer — not urgent, any morning next week works. No access issues.",
        persona = "tech-savvy homeowner who provides all details upfront and gets impatient if asked to repeat them",
        behaviorGuidelines = """
            - Your name is Jordan Lee, address is 88 Birchwood Drive
            - You already stated everything upfront; answer curtly if the agent re-asks for info you already provided
            - Confirm booking on first offer
            - Give rating 4 when asked
        """.trimIndent(),
        criterion = APPOINTMENT_SCHEDULED,
    )

    @Test
    fun `Emergency - Plumbing burst pipe flooding`() = runCase(
        scenarioName = "Emergency — Plumbing burst pipe",
        initialMessage = "Water is spraying from a pipe under my sink — it's flooding the kitchen!",
        persona = "panicked homeowner with a burst pipe causing active flooding, needs immediate help",
        behaviorGuidelines = """
            - Your name is Sam Rivera, address is 200 Elm Street
            - Express urgency clearly; water is actively leaking and flooding
        """.trimIndent(),
        criterion = EMERGENCY_REFERRAL,
    )

    private fun runCase(
        scenarioName: String,
        initialMessage: String,
        persona: String,
        behaviorGuidelines: String,
        criterion: EvaluationCriterion,
    ) {
        val simulatedUser = LLMSimulatedUser.builder()
            .judge(judge)
            .persona(persona)
            .behaviorGuidelines(behaviorGuidelines)
            .build()

        val conversation = mutableListOf<Pair<String, String>>()

        fun addMessage(role: String, content: String) {
            conversation.add(role to content)
            println("\n$role: $content")
            System.out.flush()
        }

        addMessage("User", initialMessage)

        val schedule = HomeServicesSchedule()
        val findTools = HomeServicesFindSlotTools(schedule)
        val bookTools = HomeServicesBookTools(schedule)
        val communicationTools = CommunicationTools { question ->
            addMessage("Assistant", question)
            val trajectory = ConversationTrajectory(
                conversation.map { (role, content) ->
                    if (role == "User") DokimosMessage.user(content) else DokimosMessage.assistant(content)
                },
                scenarioName,
                emptyMap<String, Any>()
            )
            val userResponse = withContext(Dispatchers.IO) {
                simulatedUser.generateMessage(trajectory).content()
            }
            addMessage("User", userResponse)
            userResponse
        }

        val agent = AIAgent(
            promptExecutor = MultiLLMPromptExecutor(llmClient),
            agentConfig = AIAgentConfig(
                prompt = prompt("home-services-scheduling") {
                    system(HomeServicesPrompts.systemPrompt())
                },
                model = model,
                maxAgentIterations = 200
            ),
            strategy = homeServicesStrategy(communicationTools, findTools, bookTools),
            toolRegistry = ToolRegistry {
                tools(communicationTools)
                tools(findTools)
                tools(bookTools)
            }
        )

        val result = runBlocking { agent.run(initialMessage) }
        addMessage("Assistant", result)

        val finalTrajectory = ConversationTrajectory(
            conversation.map { (role, content) ->
                if (role == "User") DokimosMessage.user(content) else DokimosMessage.assistant(content)
            },
            scenarioName,
            emptyMap<String, Any>()
        )

        val evaluator = trajectoryEvaluator(judge) {
            name = "$scenarioName - ${criterion.name}"
            threshold = 0.7
            criteria(listOf(criterion))
        }
        val evalResult = evaluator.evaluate(EvalTestCase(actualOutputs = mapOf("trajectory" to finalTrajectory)))
        println("\nscore=${evalResult.score()} passed=${evalResult.success()} reason=${evalResult.reason()}")
        assertTrue(evalResult.success(), "${criterion.name} check failed: ${evalResult.reason()}")
    }

    companion object {
        private val APPOINTMENT_SCHEDULED = EvaluationCriterion(
            "Appointment Scheduling",
            "The conversation resulted in a confirmed and booked home service appointment with a specific date, time window, and address.",
            1.0
        )

        private val EMERGENCY_REFERRAL = EvaluationCriterion(
            "Emergency Services Referral",
            "Instead of proceeding with appointment scheduling, the agent explicitly advised the user to call 112 (emergency services) due to the dangerous or urgent nature of the situation.",
            1.0
        )
    }
}
