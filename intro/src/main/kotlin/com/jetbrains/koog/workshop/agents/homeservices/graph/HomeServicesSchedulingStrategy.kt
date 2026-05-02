package com.jetbrains.koog.workshop.agents.homeservices.graph

import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import com.jetbrains.koog.workshop.agents.util.AskUserTool
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesBookTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesFindTools
import com.jetbrains.koog.workshop.agents.homeservices.ServiceType
import com.jetbrains.koog.workshop.agents.homeservices.UrgencyLevel
import kotlinx.serialization.Serializable

@LLMDescription("Result of the emergency check phase")
@Serializable
enum class EmergencyCheckResult {
    @LLMDescription("The emergency has been detected and the user has been informed; do not proceed with scheduling")
    EMERGENCY_DETECTED,
    @LLMDescription("No emergency detected; proceed with regular appointment scheduling")
    PROCEED_WITH_SCHEDULING,
    @LLMDescription("User cancelled the scheduling process")
    CANCELLED,
}

@LLMDescription("Collected details required to schedule a home service visit")
@Serializable
data class IntakeResult(
    @LLMDescription("Type of home service needed: PLUMBING, ELECTRICAL, HVAC, or HANDYMAN")
    val serviceType: ServiceType,
    @LLMDescription("One-sentence description of the issue to be resolved")
    val issueSummary: String,
    @LLMDescription("Full service address")
    val address: String,
    @LLMDescription("Customer's full name")
    val customerName: String,
    @LLMDescription("Urgency level: URGENT (significant disruption, within 2 days) or STANDARD (non-critical, scheduled)")
    val urgencyLevel: UrgencyLevel,
    @LLMDescription("Optional access instructions such as gate code, pet, parking, or buzzer notes")
    val accessNotes: String? = null,
)

@LLMDescription("Outcome of the intake assessment phase: either all details collected or user cancelled")
@Serializable
data class AssessResult(
    @LLMDescription("Intake details collected by the agent, if any")
    val collected: IntakeResult?,
    @LLMDescription("Whether the user chose to cancel the scheduling process")
    val cancelled: Boolean,
) {
    fun success() = collected != null && !cancelled
}

@LLMDescription("Outcome of the slot selection phase: either a slot was chosen or user cancelled")
@Serializable
data class SelectSlotResult(
    @LLMDescription("The slot selected by the customer, if any")
    val selected: SelectedSlot?,
    @LLMDescription("Whether the user chose to cancel the scheduling process")
    val cancelled: Boolean,
) {
    fun success() = selected != null && !cancelled
}

@LLMDescription("An available appointment slot selected by the customer")
@Serializable
data class SelectedSlot(
    @LLMDescription("Unique slot identifier returned by getAvailableSlots")
    val slotId: String,
    @LLMDescription("Appointment date in yyyy-MM-dd format")
    val date: String,
    @LLMDescription("Time window: Morning (9:00-12:00), Early afternoon (12:00-15:00), or Late afternoon (15:00-18:00)")
    val timeWindow: String
)

@LLMDescription("Customer's confirmation decision for the proposed appointment slot")
@Serializable
enum class ConfirmationStatus {
    @LLMDescription("Customer confirmed the slot and wants to proceed with booking")
    CONFIRMED,
    @LLMDescription("Customer wants to pick a different slot")
    CHANGE_REQUESTED,
    @LLMDescription("Customer cancelled the scheduling process")
    CANCELLED,
}

fun homeServicesSchedulingStrategy(
    askUserTool: AskUserTool,
    findTools: HomeServicesFindTools,
    bookTools: HomeServicesBookTools,
) = strategy<String, String>("home-services-scheduling") {
    val selectedSlotKey = createStorageKey<SelectedSlot>("selected-slot")
    val intakeResultKey = createStorageKey<IntakeResult>("intake-results")

    // Phase 0: check whether the request is an emergency before any scheduling
    val checkEmergency by subgraphWithTask<String, EmergencyCheckResult>(
        tools = askUserTool.asTools()
    ) { input ->
        """
        $homeServicesEmergencyCheckInstructions

        The user's initial message: $input
        """.trimIndent()
    }

    // Phase 1: gather service details from the user (no search or booking tools)
    val assess by subgraphWithTask<EmergencyCheckResult, AssessResult>(
        tools = askUserTool.asTools()
    ) { _ ->
        """
        $homeServicesIntakeInstructions

        The user's initial message: ${agentInput<String>()}
        """.trimIndent()
    }
    val storeIntake by node<IntakeResult, String> { intake ->
        storage.set(intakeResultKey, intake)
        "Intake details stored; proceeding to slot selection."
    }

    val compressHistory by nodeLLMCompressHistory<String>()

    // Phase 2: find slots and let the user pick one (no booking tool available)
    val selectSlot by subgraphWithTask<String, SelectSlotResult>(
        tools = askUserTool.asTools() + findTools.asTools()
    ) { state ->

        val intake = storage.getValue(intakeResultKey)
        """
        $homeServicesSlotSelectionInstructions

        Agent state: $state

        Intake results:
        - Customer: ${intake.customerName}
        - Service type: ${intake.serviceType}
        - Issue: ${intake.issueSummary}
        - Urgency: ${intake.urgencyLevel}
        - Address: ${intake.address}
        ${intake.accessNotes?.let { "- Access notes: $it" } ?: ""}
        """.trimIndent()
    }
    val storeSlot by node<SelectedSlot, String> { slot ->
        storage.set(selectedSlotKey, slot)
        "Slot selected and stored for booking."
    }

    // Phase 3: confirm the chosen date and time
    val confirmSlot by subgraphWithTask<String, ConfirmationStatus>(
        tools = askUserTool.asTools()
    ) { state ->

        val intake = storage.getValue(intakeResultKey)
        val slot = storage.getValue(selectedSlotKey)

        """
        $homeServicesConfirmationInstructions
        
        Agent state: $state

        Selected slot:
        - Date: ${slot.date}
        - Time window: ${slot.timeWindow}
        
        - Service type: ${intake.serviceType}
        - Customer: ${intake.customerName}
        - Address: ${intake.address}
        ${intake.accessNotes?.let { "- Notes: $it" } ?: ""}
        """.trimIndent()
    }

    // Phase 4: book the appointment programmatically — all data is already collected
    val book by node<String, String> { _ ->
        val intake = storage.getValue(intakeResultKey)
        val slot = storage.getValue(selectedSlotKey)
        bookTools.scheduleAppointment(
            customerName = intake.customerName,
            serviceType = intake.serviceType,
            slotId = slot.slotId,
            address = intake.address,
            issueDescription = intake.issueSummary,
            notes = intake.accessNotes ?: "",
        )
    }

    // Phase 5: thank the user and ask for a satisfaction rating
    val finish by subgraphWithTask<String, String>(
        tools = askUserTool.asTools()
    ) { previousResult ->
        """
        $homeServicesFinishInstructions

        Conversation outcome:
        $previousResult
        """.trimIndent()
    }

    // Cancellation path: single LLM call to thank and close the conversation
    val handleCancellation by node<String, String> { _ ->
        llm.writeSession {
            appendPrompt { user(homeServicesCancellationInstructions) }
            requestLLM().content
        }
    }

    // Cancellation path: single LLM call to close the conversation
    val handleEmergency by node<String, String> { _ ->
        llm.writeSession {
            appendPrompt { user(homeServicesEmergencyInstructions) }
            requestLLM().content
        }
    }

    nodeStart then checkEmergency
    edge(checkEmergency forwardTo assess onCondition { it == EmergencyCheckResult.PROCEED_WITH_SCHEDULING })
    edge(checkEmergency forwardTo handleCancellation onCondition { it == EmergencyCheckResult.CANCELLED } transformed { "Cancelled" })
    edge(checkEmergency forwardTo handleEmergency onCondition { it == EmergencyCheckResult.EMERGENCY_DETECTED } transformed { "Handling emergency" })

    edge(assess forwardTo storeIntake onCondition { it.success() } transformed { it.collected!! })
    edge(assess forwardTo handleCancellation onCondition { !it.success() } transformed { "Cancelled" })

    storeIntake then compressHistory
    compressHistory then selectSlot

    edge(selectSlot forwardTo storeSlot onCondition { it.success() } transformed { it.selected!! })
    edge(selectSlot forwardTo handleCancellation onCondition { !it.success() } transformed { "Cancelled" })

    storeSlot then confirmSlot

    edge(confirmSlot forwardTo selectSlot onCondition { it == ConfirmationStatus.CHANGE_REQUESTED } transformed { "Slot was selected, but the change was requested." })
    edge(confirmSlot forwardTo handleCancellation onCondition { it == ConfirmationStatus.CANCELLED } transformed { "Cancelled" })
    edge(confirmSlot forwardTo book onCondition { it == ConfirmationStatus.CONFIRMED } transformed { "Slot confirmed, proceeding to booking." })

    book then finish then nodeFinish
    handleCancellation then nodeFinish
    handleEmergency then nodeFinish
}
