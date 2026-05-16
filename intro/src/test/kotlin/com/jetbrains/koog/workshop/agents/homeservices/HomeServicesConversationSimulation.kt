package com.jetbrains.koog.workshop.agents.homeservices

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import com.jetbrains.koog.workshop.agents.homeservices.EvaluationCriteria.appointmentScheduled
import com.jetbrains.koog.workshop.agents.homeservices.EvaluationCriteria.cancelsNonrelevantRequest
import com.jetbrains.koog.workshop.agents.homeservices.EvaluationCriteria.emergencyReferral
import com.jetbrains.koog.workshop.agents.homeservices.EvaluationCriteria.gracefulCancellation
import com.jetbrains.koog.workshop.agents.homeservices.EvaluationCriteria.immediatelyCancelsOnWeekendOnly
import com.jetbrains.koog.workshop.agents.homeservices.EvaluationCriteria.noRedundantQuestions
import com.jetbrains.koog.workshop.agents.homeservices.EvaluationCriteria.refusesOffTopicQuestions
import com.jetbrains.koog.workshop.agents.homeservices.basic.CONVERSATION_END_MARKER
import com.jetbrains.koog.workshop.agents.homeservices.basic.homeServicesBasicSystemPrompt
import com.jetbrains.koog.workshop.agents.homeservices.graph.HomeServicesPrompts
import com.jetbrains.koog.workshop.agents.homeservices.graph.homeServicesStrategy
import com.jetbrains.koog.workshop.agents.util.AskUserTool
import com.jetbrains.koog.workshop.settings.ApiKeyService
import com.jetbrains.koog.workshop.settings.ApiKeyService.ServiceProvider
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
import java.io.File
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue
import dev.dokimos.core.conversation.Message as DokimosMessage

object EvaluationCriteria {
    val appointmentScheduled = EvaluationCriterion(
        "Appointment Scheduling",
        "The conversation resulted in a confirmed and booked home service appointment with a specific date, time window, and address. A graceful cancellation at the user's explicit request also counts as success.",
        1.0
    )

    val emergencyReferral = EvaluationCriterion(
        "Emergency Services Referral",
        "Instead of proceeding with appointment scheduling, the agent explicitly advised the user to call 112 (emergency services) or contact an emergency plumber or electrician hotline due to the dangerous or urgent nature of the situation.",
        1.0
    )

    val gracefulCancellation = EvaluationCriterion(
        "Graceful Cancellation",
        "After the user cancelled, the agent acknowledged the cancellation gracefully, " +
                "mentioned that the user can start a new conversation whenever they are ready, " +
                "and did not ask for a satisfaction rating.",
        1.0
    )

    val noRedundantQuestions = EvaluationCriterion(
        "No Redundant Questions",
        "The agent did not ask for information that the user already provided in their initial message or previous responses. " +
                "The agent should recognize and use information already given (such as name, address, service type, preferred time, access notes) " +
                "without requesting it again.",
        1.0
    )

    val refusesOffTopicQuestions = EvaluationCriterion(
        "Refuses Off-Topic Questions",
        "When the user asks questions unrelated to home services scheduling (such as weather, recipes, general advice, etc.), " +
                "the agent politely declines to answer and redirects the conversation back to scheduling a home service appointment.",
        1.0
    )

    val cancelsNonrelevantRequest = EvaluationCriterion(
        "Cancels Nonrelevant Request",
        "When the user requests a service that is not offered by the agent, the agent finishes the conversation.",
        1.0
    )

    val immediatelyCancelsOnWeekendOnly = EvaluationCriterion(
        "Immediate Cancellation on Weekend-Only",
        "When the user states they can only do weekends and will find another provider, the agent must immediately acknowledge the cancellation and close the conversation. " +
                "The agent must NOT continue collecting information such as name, address, or access notes after the user has made clear they are not proceeding.",
        1.0
    )

    // TODO: see if we can get the details from the tool call
    val confirmationIncludesAllDetails = EvaluationCriterion(
        "Confirmation Includes All Details",
        "When the agent asks for appointment confirmation, the confirmation must include all the details that the user mentioned during the conversation, including: name, address (with unit/floor if provided), service type, date, time window, and any access notes (buzzer codes, parking instructions, floor access, special requirements).",
        1.0
    )
}

data class SimulationCase(
    val id: String,
    val scenarioName: String,
    val initialMessage: String,
    val persona: String,
    val behaviorGuidelines: String,
    val evaluations: List<EvaluationCriterion> = emptyList(),
    val setupSchedule: ((HomeServicesSchedule) -> Unit)? = null,
)

abstract class HomeServicesConversationSimulationBase {

    protected lateinit var llmClient: LLMClient
    protected lateinit var model: LLModel
    protected lateinit var judge: JudgeLM

    protected open val writeToFile = true
    protected abstract val logSubDir: String

    /**
     * Drives the agent-user interaction for a simulation case.
     * Implementations call [addMessage] to record messages and [simulateUserResponse]
     * to obtain the next simulated user reply.
     */
    protected abstract suspend fun driveConversation(
        case: SimulationCase,
        schedule: HomeServicesSchedule,
        addMessage: (role: String, content: String) -> Unit,
        simulateUserResponse: suspend () -> String,
    )

    @Before
    fun setup() {
        when (ApiKeyService.serviceProvider) {
            ServiceProvider.OPENAI -> {
                llmClient = OpenAILLMClient(ApiKeyService.openAIApiKey)
                model = OpenAIModels.Chat.GPT4o
            }
            ServiceProvider.ANTHROPIC -> {
                llmClient = AnthropicLLMClient(ApiKeyService.anthropicApiKey)
                model = AnthropicModels.Sonnet_4
            }
            ServiceProvider.GOOGLE -> {
                llmClient = GoogleLLMClient(ApiKeyService.geminiKey)
                model = GoogleModels.Gemini2_5FlashLite
            }
        }
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

    @Test fun `Scheduling-1 - Baseline - Plumbing leaking kitchen faucet`() = runCase(SimulationCase(
        id = "Scheduling-1",
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
        evaluations = listOf(appointmentScheduled, noRedundantQuestions),
    ))

    @Test fun `Scheduling-2 - Over-Informer - HVAC tune-up`() = runCase(SimulationCase(
        id = "Scheduling-2",
        scenarioName = "Over-Informer — HVAC tune-up",
        initialMessage = "Hi, I'm Jordan Lee at 88 Birchwood Drive. I need an HVAC tune-up before summer — not urgent, any morning next week works. No access issues.",
        persona = "tech-savvy homeowner who provides all details upfront and gets impatient if asked to repeat them",
        behaviorGuidelines = """
            - Your name is Jordan Lee, address is 88 Birchwood Drive
            - You already stated everything upfront; answer curtly if the agent re-asks for info you already provided
            - Confirm booking on first offer
            - Give rating 4 when asked
        """.trimIndent(),
        evaluations = listOf(appointmentScheduled, noRedundantQuestions),
    ))

    @Test fun `Rescheduling-1 - Indecisive Rescheduler - Plumbing slow drain`() = runCase(SimulationCase(
        id = "Rescheduling-1",
        scenarioName = "Indecisive Rescheduler — Plumbing slow drain",
        initialMessage = "My bathroom drain has been draining really slowly for a week.",
        persona = "homeowner who changes mind about timing after seeing slot options, triggering a re-selection loop",
        behaviorGuidelines = """
            - Your name is Maria Chen, address is 15 Oak Lane, Apt 3B
            - Request morning first, then switch to late afternoon after slots are shown
            - At the confirmation step, say you want an earlier date, if possible today, and ask to see different slots
            - Accept the new slot on the second confirmation
            - Give rating 4 when asked
        """.trimIndent(),
        evaluations = listOf(appointmentScheduled),
    ))

    @Test fun `Emergency-1 - Emergency Caller - Plumbing burst pipe`() = runCase(SimulationCase(
        id = "Emergency-1",
        scenarioName = "Emergency Caller — Plumbing burst pipe",
        initialMessage = "Water is spraying from a pipe under my sink — it's flooding the kitchen!",
        persona = "panicked homeowner with a burst pipe causing active flooding, needs immediate help",
        behaviorGuidelines = """
            - Your name is Sam Rivera, address is 200 Elm Street
            - Express urgency clearly; water is actively leaking and flooding
            - Accept the earliest available slot without negotiating
            - Give rating 5 when asked
        """.trimIndent(),
        evaluations = listOf(emergencyReferral),
    ))

    @Test fun `Emergency-2 - Safety Emergency - Electrical sparking panel`() = runCase(SimulationCase(
        id = "Emergency-2",
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
        evaluations = listOf(emergencyReferral),
    ))

    @Test
    fun `Emergency-3 - False Alarm - Gas smell that turns out to be garbage`() = runCase(
        SimulationCase(
            id = "Emergency-3",
            scenarioName = "False Alarm — Gas smell that turns out to be garbage",
            initialMessage = "There's a strange smell in my kitchen and I'm worried there might be a leak!",
            persona = "homeowner who initially reports a strange smell (may be gas?) but after explaining realizes it's coming from the garbage disposal",
            behaviorGuidelines = """
        - Your name is Taylor Bennett, address is 92 Oakwood Lane
        - Initially express concern about a strange smell
        - When asked to describe the smell or location more precisely, clarify that it's actually coming from the garbage disposal and smells like rotting food
        - Accept that this is not an emergency and proceed with normal scheduling for a plumbing service to check the disposal
        - Prefer afternoon appointments
        - Give rating 4 when asked
    """.trimIndent(),
            evaluations = listOf(appointmentScheduled),
        )
    )

    @Test fun `Emergency-4 - Ambiguous Flooding - Water on bathroom floor turns out to be burst pipe`() = runCase(SimulationCase(
        id = "Emergency-4",
        scenarioName = "Ambiguous Flooding — Water on bathroom floor turns out to be a burst pipe",
        initialMessage = "There's water all over my bathroom floor.",
        persona = "homeowner whose pipe under the bathroom vanity just burst, water actively spreading",
        behaviorGuidelines = """
            - Your name is Jamie Torres, address is 14 Birch Lane
            - When the agent asks where the water is coming from or for more details, explain it is spraying from a pipe under the bathroom vanity and spreading across the floor
            - Confirm the water is still actively coming out and getting worse
            - After the agent advises calling emergency services, acknowledge you will call immediately
        """.trimIndent(),
        evaluations = listOf(emergencyReferral),
    ))

    @Test fun `Emergency-5 - Ambiguous Electrical - Buzzing panel turns out not to be emergency`() = runCase(SimulationCase(
        id = "Emergency-5",
        scenarioName = "Ambiguous Electrical — Buzzing panel turns out not to be an emergency",
        initialMessage = "My electrical panel is buzzing loudly.",
        persona = "homeowner with a buzzing electrical panel but no sparks or burning smell",
        behaviorGuidelines = """
            - Your name is Robin Sanders, address is 27 Elmwood Drive
            - When the agent asks for more details (sparks, burning smell, etc.), confirm there are no sparks and no burning smell — just a persistent buzzing sound that started a couple of days ago
            - Accept the first available electrical slot
            - Give rating 4 when asked
        """.trimIndent(),
        evaluations = listOf(appointmentScheduled),
    ))

    @Test fun `Scheduling-3 - Apartment Renter with Access Constraints - Handyman shelves`() = runCase(SimulationCase(
        id = "Scheduling-3",
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
        evaluations = listOf(appointmentScheduled, noRedundantQuestions),
    ))

    @Test fun `Scheduling-4 - Vague Describer - HVAC furnace not starting`() = runCase(SimulationCase(
        id = "Scheduling-4",
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
        evaluations = listOf(appointmentScheduled, noRedundantQuestions),
    ))

    @Test fun `Scheduling-Saturday - Weekend Requester - Handyman squeaky door`() = runCase(SimulationCase(
        id = "Scheduling-Saturday",
        scenarioName = "Weekend Requester — Handyman squeaky door",
        initialMessage = "Can I get a handyman to come this Saturday to fix a squeaky door?",
        persona = "homeowner who specifically requests a Saturday appointment and accepts a weekday alternative",
        behaviorGuidelines = """
            - Your name is Ryan Patel, address is 12 Ferndale Road
            - Request Saturday specifically
            - When told Saturday is unavailable, accept the first weekday morning slot offered
            - Give rating 3 when asked
        """.trimIndent(),
        evaluations = listOf(appointmentScheduled, noRedundantQuestions),
    ))

    @Test
    fun `Scheduling-Saturday-2 - Weekend Requester Who Cancels - Handyman door hinge`() = runCase(
        SimulationCase(
            id = "Scheduling-Saturday-2",
            scenarioName = "Weekend Requester Who Cancels — Handyman door hinge",
            initialMessage = "I need a handyman this Saturday to fix a loose door hinge.",
            persona = "homeowner who specifically requests a Saturday appointment but cancels when told Saturday is unavailable",
            behaviorGuidelines = """
        - Your name is Avery Clarke, address is 34 Willow Street
        - Request Saturday specifically
        - When told Saturday is unavailable, cancel: say you can only do weekends and will find another provider
    """.trimIndent(),
            evaluations = listOf(gracefulCancellation, immediatelyCancelsOnWeekendOnly),
        )
    )

    @Test fun `Scheduling-5 - Busy Schedule - Electrical outlet`() = runCase(SimulationCase(
        id = "Scheduling-5",
        scenarioName = "Busy Schedule / Limited Availability — Electrical outlet",
        initialMessage = "One of my kitchen outlets stopped working.",
        persona = "homeowner booking electrical service when most early slots are already taken",
        behaviorGuidelines = """
            - Your name is Leslie Kim, address is 78 Birch Street
            - Accept the first slot offered without negotiating
            - Give rating 4 when asked
        """.trimIndent(),
        evaluations = listOf(appointmentScheduled),
        setupSchedule = { schedule ->
            schedule.slots
                .filter { ServiceType.ELECTRICAL in it.specialistType.supportedServices }
                .take(8)
                .forEach { slot ->
                    schedule.bookAnAppointment(slot.id, Booking("Pre-booked", "Pre-booked address"))
                }
        },
    ))

    @Test fun `Scheduling-6 - Commercial Property Client - HVAC office AC not cooling`() = runCase(SimulationCase(
        id = "Scheduling-6",
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
        evaluations = listOf(appointmentScheduled),
    ))

    @Test fun `Urgency-1 - Urgent Toilet - Only bathroom not flushing`() = runCase(SimulationCase(
        id = "Urgency-1",
        scenarioName = "Urgent Toilet — Only bathroom not flushing",
        initialMessage = "My toilet isn't flushing and I can't get it to work.",
        persona = "homeowner whose only toilet is broken and needs it fixed as soon as possible",
        behaviorGuidelines = """
            - Your name is Casey Morgan, address is 18 Linden Street
            - When asked, confirm this is your only bathroom
            - Accept the first available slot
            - Give rating 5 when asked
        """.trimIndent(),
        evaluations = listOf(
            appointmentScheduled,
            EvaluationCriterion(
                "Urgency Assessment",
                "The agent asked how many bathrooms the home has, identified that the broken toilet is the only one, " +
                    "and treated the request as urgent — offering appointment slots within the next 2 days.",
                1.0,
            ),
        ),
    ))

    @Test fun `Urgency-2 - Standard Toilet - Two bathroom home, one toilet not flushing`() = runCase(SimulationCase(
        id = "Urgency-2",
        scenarioName = "Standard Toilet — Two-bathroom home, one toilet not flushing",
        initialMessage = "My toilet stopped flushing properly.",
        persona = "homeowner with two bathrooms, one toilet broken — no real urgency",
        behaviorGuidelines = """
            - Your name is Drew Sullivan, address is 55 Maple Court
            - When asked, confirm you have two bathrooms and the other toilet works fine
            - Accept the first available slot
            - Give rating 4 when asked
        """.trimIndent(),
        evaluations = listOf(
            appointmentScheduled,
            EvaluationCriterion(
                "Urgency Assessment",
                "The agent asked how many bathrooms the home has, identified that a second working bathroom is available, " +
                    "and treated the request as standard (non-urgent) — offering appointment slots starting 2 or more business days from today.",
                1.0,
            ),
        ),
    ))

    @Test fun `Cancel-1 - Confirmation Canceller - Plumbing intermittent drip`() = runCase(SimulationCase(
        id = "Cancel-1",
        scenarioName = "Canceller — Plumbing intermittent drip",
        initialMessage = "There's a small drip under my bathroom sink, not sure if it needs fixing urgently.",
        persona = "homeowner who proceeds through the flow but cancels at the confirmation step",
        behaviorGuidelines = """
            - Your name is Dana Ortiz, address is 99 Walnut Court
            - Proceed normally through information gathering and slot selection
            - At the confirmation step, cancel: say you will hold off for now and try to fix it yourself
            - Do not give a rating since you cancelled before booking
        """.trimIndent(),
        evaluations = listOf(gracefulCancellation),
    ))

    @Test fun `Cancel-2 - Early Canceller - Cancels during intake`() = runCase(SimulationCase(
        id = "Cancel-2",
        scenarioName = "Early Canceller — Cancels during intake",
        initialMessage = "I need a plumber to fix a leak.",
        persona = "homeowner who starts the booking process but changes mind partway through gathering details",
        behaviorGuidelines = """
            - Answer the first question from the agent, then cancel: say you have decided not to proceed for now
        """.trimIndent(),
        evaluations = listOf(gracefulCancellation),
    ))

    @Test fun `Cancel-3 - Slot Canceller - Cancels after seeing available slots`() = runCase(SimulationCase(
        id = "Cancel-3",
        scenarioName = "Slot Canceller — Cancels after seeing available slots",
        initialMessage = "I need an electrician to look at a tripping breaker.",
        persona = "homeowner who provides all details but decides to cancel once appointment slots are presented",
        behaviorGuidelines = """
            - Your name is Quinn Foster, address is 21 Riverbank Drive
            - Provide all requested details without hesitation
            - When the agent presents available appointment slots, cancel: say none of the times work and you will call back later
        """.trimIndent(),
        evaluations = listOf(gracefulCancellation),
    ))

    @Test
    fun `Off-Topic-1 - Refuses Irrelevant Questions - Weather inquiry`() = runCase(
        SimulationCase(
            id = "Off-Topic-1",
            scenarioName = "Refuses Irrelevant Questions — Weather and recipe inquiry",
            initialMessage = "I need a plumber to fix a leaking pipe.",
            persona = "homeowner who starts booking a plumber but then asks an off-topic question about the weather and a recipe",
            behaviorGuidelines = """
            - Your name is Jamie Cooper, address is 66 Cedar Lane
            - Share your name and address in one message when asked only about the name
            - Answer the first question from the agent normally
            - After providing one piece of information, ask: "Can you give me a recipe for chocolate chip cookies?"
            - After the agent refuses and redirects, continue with the booking process normally
            - Then ask about the current weather
            - Accept the first available slot
            - Give rating 4 when asked
        """.trimIndent(),
            evaluations = listOf(appointmentScheduled, refusesOffTopicQuestions),
        )
    )

    @Test
    fun `Off-Topic-2 - Refuses Irrelevant Questions - Recipe inquiry from start`() = runCase(
        SimulationCase(
            id = "Off-Topic-2",
            scenarioName = "Refuses Irrelevant Questions — Recipe inquiry from start",
            initialMessage = "I need to get my hair cut.",
            persona = "person who wants an unrelated service",
            behaviorGuidelines = """
            - After the agent refuses and redirects to home services, ask whether the requested service is hairdressing.
            - After the agent replies, say that you were told it is a hairdressing service.
            - Then politely say goodbye and end the conversation.
        """.trimIndent(),
            evaluations = listOf(cancelsNonrelevantRequest),
        )
    )

    protected fun runCase(case: SimulationCase) {
        val simulatedUser = LLMSimulatedUser.builder()
            .judge(judge)
            .persona(case.persona)
            .behaviorGuidelines(case.behaviorGuidelines)
            .build()

        val conversation = mutableListOf<Pair<String, String>>()

        fun addMessage(role: String, content: String) {
            conversation.add(role to content)
            println("\n$role: $content")
            System.out.flush()
        }

        addMessage("User", case.initialMessage)

        val schedule = HomeServicesSchedule()
        case.setupSchedule?.invoke(schedule)

        val simulateUserResponse: suspend () -> String = {
            val trajectory = ConversationTrajectory(
                conversation.map { (role, content) ->
                    if (role == "User") DokimosMessage.user(content) else DokimosMessage.assistant(content)
                },
                case.scenarioName,
                emptyMap<String, Any>()
            )
            withContext(Dispatchers.IO) { simulatedUser.generateMessage(trajectory).content() }
        }

        runBlocking {
            driveConversation(case, schedule, ::addMessage, simulateUserResponse)
        }

        if (writeToFile) {
            writeConversationToAFile("logs/conversation-simulation/$logSubDir/${case.id}.txt", case, conversation)
        }

        val finalTrajectory = ConversationTrajectory(
            conversation.map { (role, content) ->
                if (role == "User") DokimosMessage.user(content) else DokimosMessage.assistant(content)
            },
            case.scenarioName,
            emptyMap<String, Any>()
        )

        case.evaluations.forEach { criterion ->
            evaluateConversationTrajectory(case, criterion, finalTrajectory)
        }
    }

    private fun writeConversationToAFile(
        fileName: String,
        case: SimulationCase,
        conversation: MutableList<Pair<String, String>>
    ) {
        val outputFile = File(fileName)
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
    }

    private fun evaluateConversationTrajectory(
        case: SimulationCase,
        criterion: EvaluationCriterion,
        finalTrajectory: ConversationTrajectory
    ) {
        val evaluator = trajectoryEvaluator(judge) {
            name = "${case.id} - ${criterion.name}"
            threshold = 0.7
            criteria(listOf(criterion))
        }
        val result = evaluator.evaluate(EvalTestCase(actualOutputs = mapOf("trajectory" to finalTrajectory)))

        println("\n${case.id} score=${result.score()} passed=${result.success()} reason=${result.reason()}")

        assertTrue(result.success(), "${case.id} ${criterion.name} check failed: ${result.reason()}")
    }
}

class HomeServicesGraphConversationSimulation : HomeServicesConversationSimulationBase() {
    override val logSubDir = "graph"

    override suspend fun driveConversation(
        case: SimulationCase,
        schedule: HomeServicesSchedule,
        addMessage: (role: String, content: String) -> Unit,
        simulateUserResponse: suspend () -> String,
    ) {
        val findTools = HomeServicesFindSlotTools(schedule)
        val bookTools = HomeServicesBookTools(schedule)
        val askUserTool = AskUserTool { question ->
            addMessage("Assistant", question)
            val userResponse = simulateUserResponse()
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
            strategy = homeServicesStrategy(askUserTool, findTools, bookTools),
            toolRegistry = ToolRegistry {
                tools(askUserTool)
                tools(findTools)
                tools(bookTools)
            }
        )
        val result = agent.run(case.initialMessage)
        addMessage("Assistant", result)
    }
}

class HomeServicesBasicConversationSimulation : HomeServicesConversationSimulationBase() {
    override val logSubDir = "basic"

    override suspend fun driveConversation(
        case: SimulationCase,
        schedule: HomeServicesSchedule,
        addMessage: (role: String, content: String) -> Unit,
        simulateUserResponse: suspend () -> String,
    ) {
        val historyProvider = InMemoryChatHistoryProvider()
        val sessionId = UUID.randomUUID().toString()
        val findTools = HomeServicesFindSlotTools(schedule)
        val bookTools = HomeServicesBookTools(schedule)
        val agent = AIAgent(
            promptExecutor = MultiLLMPromptExecutor(llmClient),
            agentConfig = AIAgentConfig(
                prompt = prompt("home-services-basic") {
                    system(homeServicesBasicSystemPrompt())
                },
                model = model,
                maxAgentIterations = 200
            ),
            toolRegistry = ToolRegistry {
                tools(findTools)
                tools(bookTools)
            }
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
            }
        }

        var userMessage = case.initialMessage
        repeat(MAX_TURNS) {
            val rawResponse = agent.run(userMessage, sessionId)
            val done = CONVERSATION_END_MARKER in rawResponse
            val response = rawResponse.replace(CONVERSATION_END_MARKER, "").trim()
            addMessage("Assistant", response)
            if (done) return
            userMessage = simulateUserResponse()
            addMessage("User", userMessage)
        }
    }

    companion object {
        private const val MAX_TURNS = 30
    }
}
