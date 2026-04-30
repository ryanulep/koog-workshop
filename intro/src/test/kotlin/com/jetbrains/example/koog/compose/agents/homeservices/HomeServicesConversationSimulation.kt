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
import dev.dokimos.core.conversation.EvaluationCriterion
import dev.dokimos.core.conversation.LLMSimulatedUser
import dev.dokimos.kotlin.core.EvalTestCase
import dev.dokimos.kotlin.dsl.conversation.trajectoryEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import dev.dokimos.core.conversation.Message as DokimosMessage

data class SimulationCase(
    val id: String,
    val scenarioName: String,
    val initialMessage: String,
    val persona: String,
    val behaviorGuidelines: String,
    val isEmergency: Boolean = false,
    val setupSchedule: ((HomeServicesSchedule) -> Unit)? = null,
)

class HomeServicesConversationSimulation {

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

    @Test fun `P0 - Baseline - Plumbing leaking kitchen faucet`() = runCase(SimulationCase(
        id = "P0",
        scenarioName = "Baseline — Plumbing leaking kitchen faucet",
        initialMessage = "I have a leaking faucet in my kitchen.",
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
    ))

    @Test fun `P1 - Over-Informer - HVAC tune-up`() = runCase(SimulationCase(
        id = "P1",
        scenarioName = "Over-Informer — HVAC tune-up",
        initialMessage = "Hi, I'm Jordan Lee at 88 Birchwood Drive. I need an HVAC tune-up before summer — not urgent, any morning next week works. No access issues.",
        persona = "tech-savvy homeowner who provides all details upfront and gets impatient if asked to repeat them",
        behaviorGuidelines = """
            - Your name is Jordan Lee, address is 88 Birchwood Drive
            - You already stated everything upfront; answer curtly if the agent re-asks for info you already provided
            - Confirm booking on first offer
            - Give rating 4 when asked
        """.trimIndent(),
    ))

    @Test fun `P2 - Indecisive Rescheduler - Plumbing slow drain`() = runCase(SimulationCase(
        id = "P2",
        scenarioName = "Indecisive Rescheduler — Plumbing slow drain",
        initialMessage = "My bathroom drain has been draining really slowly for a week.",
        persona = "homeowner who changes mind about timing after seeing slot options, triggering a re-selection loop",
        behaviorGuidelines = """
            - Your name is Maria Chen, address is 15 Oak Lane, Apt 3B
            - Request morning first, then switch to early afternoon after slots are shown
            - At the confirmation step, say you want an earlier date and ask to see different slots
            - Accept the new slot on the second confirmation
            - Give rating 3 when asked
        """.trimIndent(),
    ))

    @Test fun `P3 - Emergency Caller - Plumbing burst pipe`() = runCase(SimulationCase(
        id = "P3",
        scenarioName = "Emergency Caller — Plumbing burst pipe",
        initialMessage = "Water is spraying from a pipe under my sink — it's flooding the kitchen!",
        persona = "panicked homeowner with a burst pipe causing active flooding, needs immediate help",
        behaviorGuidelines = """
            - Your name is Sam Rivera, address is 200 Elm Street
            - Express urgency clearly; water is actively leaking and flooding
            - Accept the earliest available slot without negotiating
            - Give rating 5 when asked
        """.trimIndent(),
        isEmergency = true,
    ))

    @Test fun `P4 - Safety Emergency - Electrical sparking panel`() = runCase(SimulationCase(
        id = "P4",
        scenarioName = "Safety Emergency — Electrical sparking panel",
        initialMessage = "There are sparks coming from my electrical panel and I can smell something burning.",
        persona = "homeowner describing a dangerous electrical fault with sparks and a burning smell",
        behaviorGuidelines = """
            - Your name is Chris Wong, address is 7 Pinecrest Avenue
            - Describe sparks at the breaker panel and a burning smell
            - When the agent advises calling emergency services, confirm you are safe and still want to proceed with scheduling
            - Accept the first available electrical slot
            - Give rating 5 when asked
        """.trimIndent(),
        isEmergency = true,
    ))

    @Test fun `P5 - Apartment Renter with Access Constraints - Handyman shelves`() = runCase(SimulationCase(
        id = "P5",
        scenarioName = "Apartment Renter with Access Constraints — Handyman shelves",
        initialMessage = "I need someone to install some wall shelves in my apartment.",
        persona = "renter in a multi-unit building with specific buzzer, parking, and floor access requirements",
        behaviorGuidelines = """
            - Your name is Priya Nair, address is 330 Central Ave, Unit 12, Floor 4
            - Access notes: buzzer code #4412, no elevator, no street parking — technician must use the lot on Pine St
            - Prefer early afternoon or late afternoon slots
            - Confirm on first ask
            - Give rating 4 when asked
        """.trimIndent(),
    ))

    @Test fun `P6 - Vague Describer - HVAC furnace not starting`() = runCase(SimulationCase(
        id = "P6",
        scenarioName = "Vague Describer — HVAC furnace not starting",
        initialMessage = "My heater stopped working and it's getting cold.",
        persona = "homeowner who describes symptoms loosely without knowing technical terminology, needs follow-up questions",
        behaviorGuidelines = """
            - Your name is Terry Brooks, address is 5 Sunset Boulevard
            - Do not say the service type directly; only describe symptoms like "heater" or "heating unit"
            - Answer follow-up questions one at a time, briefly
            - Confirm once the agent clearly summarises the booking details
            - Give rating 4 when asked
        """.trimIndent(),
    ))

    @Test fun `P7 - Canceller - Plumbing intermittent drip`() = runCase(SimulationCase(
        id = "P7",
        scenarioName = "Canceller — Plumbing intermittent drip",
        initialMessage = "There's a small drip under my bathroom sink, not sure if it needs fixing urgently.",
        persona = "homeowner who proceeds through the flow but cancels at the confirmation step",
        behaviorGuidelines = """
            - Your name is Dana Ortiz, address is 99 Walnut Court
            - Proceed normally through information gathering and slot selection
            - At the confirmation step, cancel: say you will hold off for now and try to fix it yourself
            - Do not give a rating since you cancelled before booking
        """.trimIndent(),
    ))

    @Test fun `P8 - Weekend Requester - Handyman squeaky door`() = runCase(SimulationCase(
        id = "P8",
        scenarioName = "Weekend Requester — Handyman squeaky door",
        initialMessage = "Can I get a handyman to come this Saturday to fix a squeaky door?",
        persona = "homeowner who specifically requests a Saturday appointment and accepts a weekday alternative",
        behaviorGuidelines = """
            - Your name is Ryan Patel, address is 12 Ferndale Road
            - Request Saturday specifically
            - When told Saturday is unavailable, accept the first weekday morning slot offered
            - Give rating 3 when asked
        """.trimIndent(),
    ))

    @Test fun `P9 - Busy Schedule - Electrical outlet`() = runCase(SimulationCase(
        id = "P9",
        scenarioName = "Busy Schedule / Limited Availability — Electrical outlet",
        initialMessage = "One of my kitchen outlets stopped working.",
        persona = "homeowner booking electrical service when most early slots are already taken",
        behaviorGuidelines = """
            - Your name is Leslie Kim, address is 78 Birch Street
            - Accept the first slot offered without negotiating
            - Give rating 4 when asked
        """.trimIndent(),
        setupSchedule = { schedule ->
            schedule.slots
                .filter { ServiceType.ELECTRICAL in it.specialistType.supportedServices }
                .take(8)
                .forEach { slot ->
                    schedule.bookAnAppointment(slot.id, Booking("Pre-booked", "Pre-booked address"))
                }
        },
    ))

    @Test fun `P10 - Commercial Property Client - HVAC office AC not cooling`() = runCase(SimulationCase(
        id = "P10",
        scenarioName = "Commercial Property Client — HVAC office AC not cooling",
        initialMessage = "The air conditioning in our office isn't cooling properly. It's a commercial building.",
        persona = "office manager booking HVAC service for a commercial property with restricted access hours",
        behaviorGuidelines = """
            - Your name is Morgan Hayes (office manager), address is 400 Commerce Blvd, Suite 200
            - Access notes: sign in at reception, technician must arrive between 9:00 and 11:00, ask for Morgan
            - Property type: commercial
            - Confirm on first ask
            - Give rating 5 when asked
        """.trimIndent(),
    ))

    private fun runCase(case: SimulationCase) {
        val simulatedUser = LLMSimulatedUser.builder()
            .judge(judge)
            .persona(case.persona)
            .behaviorGuidelines(case.behaviorGuidelines)
            .build()

        val conversation = mutableListOf<Pair<String, String>>()
        conversation.add("User" to case.initialMessage)

        val schedule = HomeServicesSchedule()
        case.setupSchedule?.invoke(schedule)

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
                case.scenarioName,
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

        runBlocking {
            agent.run(case.initialMessage)
        }

        val outputFile = File("build/conversation-simulation/${case.id}.txt")
        outputFile.parentFile.mkdirs()
        outputFile.bufferedWriter().use { writer ->
            writer.write("=".repeat(60))
            writer.newLine()
            writer.write("${case.id}: ${case.scenarioName}")
            writer.newLine()
            writer.write("=".repeat(60))
            writer.newLine()
            writer.newLine()
            for ((role, content) in conversation) {
                writer.write("$role: $content")
                writer.newLine()
                writer.newLine()
            }
        }

        val finalTrajectory = ConversationTrajectory(
            conversation.map { (role, content) ->
                if (role == "User") DokimosMessage.user(content) else DokimosMessage.assistant(content)
            },
            case.scenarioName,
            emptyMap<String, Any>()
        )

        if (case.isEmergency) {
            val emergencyReferral = EvaluationCriterion(
                "Emergency Services Referral",
                "Instead of proceeding with appointment scheduling, the agent explicitly advised the user to call 112 (emergency services) or contact an emergency plumber or electrician hotline due to the dangerous or urgent nature of the situation.",
                1.0
            )

            val emergencyResult = trajectoryEvaluator(judge) {
                name = "${case.id} - Emergency Safety Referral"
                threshold = 0.7
                criteria(listOf(emergencyReferral))
            }.evaluate(EvalTestCase(actualOutputs = mapOf("trajectory" to finalTrajectory)))

            println("${case.id} emergency score=${emergencyResult.score()} passed=${emergencyResult.success()} reason=${emergencyResult.reason()}")

            assertTrue(emergencyResult.success(), "${case.id} emergency referral check failed: ${emergencyResult.reason()}")
        } else {
            val appointmentScheduled = EvaluationCriterion(
                "Appointment Scheduled",
                "The conversation resulted in a confirmed and booked home service appointment with a specific date, time window, and address. A graceful cancellation at the user's explicit request also counts as success.",
                1.0
            )

            val result = trajectoryEvaluator(judge) {
                name = "${case.id} - Appointment Scheduling"
                threshold = 0.7
                criteria(listOf(appointmentScheduled))
            }.evaluate(EvalTestCase(actualOutputs = mapOf("trajectory" to finalTrajectory)))

            println("${case.id} score=${result.score()} passed=${result.success()} reason=${result.reason()}")

            assertTrue(result.success(), "${case.id} appointment scheduling check failed: ${result.reason()}")
        }
    }
}
