@file:Suppress("EnumEntryName")

package com.jetbrains.koog.workshop.agents.homeservices.graph

import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.message.MessagePart
import com.jetbrains.koog.workshop.agents.util.CommunicationTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesBookTools
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesFindSlotTools
import com.jetbrains.koog.workshop.agents.homeservices.ServiceType
import com.jetbrains.koog.workshop.agents.homeservices.UrgencyLevel
import kotlinx.serialization.Serializable

@LLMDescription("Result of the emergency triage phase")
@Serializable
data class TriageResult(
    @LLMDescription("The outcome of the emergency triage")
    val status: TriageOutcome,
    @LLMDescription("Brief justification of why this is an emergency. Required when status is EMERGENCY_DETECTED, null otherwise.")
    val justification: String? = null,
)

@Serializable
enum class TriageOutcome {
    @LLMDescription("Emergency detected; justification must be provided")
    emergency_detected,
    @LLMDescription("No emergency detected; proceed with regular appointment scheduling")
    proceed_with_scheduling,
    @LLMDescription("User cancelled the scheduling process")
    cancelled,
}

@LLMDescription("Collected details required to schedule a home service visit")
@Serializable
data class IssueDetails(
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
    @LLMDescription("Optional time preference volunteered by the user (e.g. 'morning', 'after 3pm', 'Wednesday'). Never ask for this — only record if the user mentions it unprompted.")
    val timePreferencesNote: String? = null,
)

@LLMDescription("Outcome of the issue details collection phase: either all details collected or user cancelled")
@Serializable
data class IssueDetailsOutcome(
    @LLMDescription("Issue details collected by the agent, if any")
    val collected: IssueDetails?,
    @LLMDescription("Whether the user chose to cancel the scheduling process")
    val cancelled: Boolean,
) {
    fun success() = collected != null && !cancelled
}

@LLMDescription("Outcome of the slot selection phase: either a slot was chosen or user cancelled")
@Serializable
data class SlotSelectionOutcome(
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

@LLMDescription("Customer's confirmation decision for the proposed appointment")
@Serializable
enum class ConfirmationOutcome {
    @LLMDescription("Customer confirmed the slot and wants to proceed with booking")
    confirmed,
    @LLMDescription("Customer wants to pick a different slot")
    change_requested,
    @LLMDescription("Customer cancelled the scheduling process")
    cancelled,
}

fun homeServicesStrategy(
    communicationTools: CommunicationTools,
    findAvailableSlotTools: HomeServicesFindSlotTools,
    bookTools: HomeServicesBookTools,
) = strategy<String, String>("home-services-scheduling") {
    val issueDetailsKey = createStorageKey<IssueDetails>("issue-details")
    val selectedSlotKey = createStorageKey<SelectedSlot>("selected-slot")

    // Phase 0: triage — detect emergencies before any scheduling
    val triageEmergency by subgraphWithTask<String, TriageResult>(
        tools = communicationTools.asTools()
    ) { input ->
        HomeServicesPrompts.triageEmergencyInstructions(input)
    }

    // Phase 1: collect issue details from the user (no search or booking tools)
    val collectIssueDetails by subgraphWithTask<TriageResult, IssueDetailsOutcome>(
        tools = communicationTools.asTools()
    ) { _ ->
        HomeServicesPrompts.collectIssueDetailsInstructions(agentInput<String>())
    }
    val storeIssueDetails by node<IssueDetails, String> { details ->
        storage.set(issueDetailsKey, details)
        "Issue details stored; proceeding to slot selection."
    }

    val compressHistory by nodeLLMCompressHistory<String>()

    // Phase 2: select a slot — find availability and let the user pick
    val selectSlot by subgraphWithTask<String, SlotSelectionOutcome>(
        tools = communicationTools.asTools() + findAvailableSlotTools.asTools()
    ) { state ->
        val issueDetails = storage.getValue(issueDetailsKey)
        HomeServicesPrompts.selectSlotInstructions(issueDetails, state)
    }
    val storeSelectedSlot by node<SelectedSlot, String> { slot ->
        storage.set(selectedSlotKey, slot)
        "Slot selected and stored for booking."
    }

    // Phase 3: confirm appointment — review details with the customer before booking
    val confirmAppointment by subgraphWithTask<String, ConfirmationOutcome>(
        tools = communicationTools.asTools()
    ) { state ->
        val issueDetails = storage.getValue(issueDetailsKey)
        val slot = storage.getValue(selectedSlotKey)
        HomeServicesPrompts.confirmAppointmentInstructions(issueDetails, slot, state)
    }

    // Phase 4: book appointment — all data is already collected, no LLM needed
    val bookAppointment by node<String, String> { _ ->
        val issueDetails = storage.getValue(issueDetailsKey)
        val slot = storage.getValue(selectedSlotKey)
        bookTools.bookAppointment(
            customerName = issueDetails.customerName,
            serviceType = issueDetails.serviceType,
            slotId = slot.slotId,
            address = issueDetails.address,
            issueDescription = issueDetails.issueSummary,
            notes = issueDetails.accessNotes ?: "",
        )
    }

    // Phase 5: collect feedback — thank the user and ask for a satisfaction rating
    val collectFeedback by subgraphWithTask<String, String>(
        tools = communicationTools.asTools()
    ) { previousResult ->
        HomeServicesPrompts.collectFeedbackInstructions(previousResult)
    }

    // Cancellation path: single LLM call to thank and close the conversation
    val handleCancellation by node<String, String> { _ ->
        llm.writeSession {
            appendPrompt { user(HomeServicesPrompts.handleCancellationInstructions) }
            // FIXME replace with textContent()
            requestLLM().parts.filterIsInstance<MessagePart.Text>().joinToString(separator = "\n") { it.text }
        }
    }

    // Emergency path: direct the user to emergency services and close
    val handleEmergency by node<String, String> { justification ->
        llm.writeSession {
            appendPrompt { user(HomeServicesPrompts.respondToEmergencyInstructions(justification)) }
            // FIXME replace with textContent()
            requestLLM().parts.filterIsInstance<MessagePart.Text>().joinToString(separator = "\n") { it.text }
        }
    }

    nodeStart then triageEmergency
    edge(triageEmergency forwardTo collectIssueDetails onCondition { it.status == TriageOutcome.proceed_with_scheduling })
    edge(triageEmergency forwardTo handleCancellation onCondition { it.status == TriageOutcome.cancelled } transformed { "Cancelled" })
    edge(triageEmergency forwardTo handleEmergency onCondition { it.status == TriageOutcome.emergency_detected } transformed { it.justification ?: "Emergency situation detected" })

    edge(collectIssueDetails forwardTo storeIssueDetails onCondition { it.success() } transformed { it.collected!! })
    edge(collectIssueDetails forwardTo handleCancellation onCondition { !it.success() } transformed { "Cancelled" })

    storeIssueDetails then compressHistory
    compressHistory then selectSlot

    edge(selectSlot forwardTo storeSelectedSlot onCondition { it.success() } transformed { it.selected!! })
    edge(selectSlot forwardTo handleCancellation onCondition { !it.success() } transformed { "Cancelled" })

    storeSelectedSlot then confirmAppointment

    edge(confirmAppointment forwardTo selectSlot onCondition { it == ConfirmationOutcome.change_requested } transformed { "Slot was selected, but the change was requested." })
    edge(confirmAppointment forwardTo handleCancellation onCondition { it == ConfirmationOutcome.cancelled } transformed { "Cancelled" })
    edge(confirmAppointment forwardTo bookAppointment onCondition { it == ConfirmationOutcome.confirmed } transformed { "Slot confirmed, proceeding to booking." })

    bookAppointment then collectFeedback then nodeFinish
    handleCancellation then nodeFinish
    handleEmergency then nodeFinish
}
