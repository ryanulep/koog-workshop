@file:Suppress("EnumEntryName")
package com.jetbrains.koog.workshop.agents.homeservices.graph

import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.ext.agent.subgraphWithTask
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesBookingProvider
import com.jetbrains.koog.workshop.agents.util.textContent

fun homeServicesStrategy(
    communicationTools: List<Tool<*, *>>,
    findAvailableSlotTools: List<Tool<*, *>>,
    bookingProvider: HomeServicesBookingProvider,
) = strategy<String, String>("home-services-scheduling") {
    val issueDetailsKey = createStorageKey<IssueDetails>("issue-details")
    val selectedSlotKey = createStorageKey<SelectedSlot>("selected-slot")

    // Phase 0: triage — detect emergencies before any scheduling
    val triageEmergency by subgraphWithTask<String, TriageResult>(
        tools = communicationTools
    ) { input ->
        HomeServicesPrompts.triageEmergencyInstructions(input)
    }

    // Phase 1: collect issue details from the user (no search or booking tools)
    val collectIssueDetails by subgraphWithTask<TriageResult, IssueDetailsOutcome>(
        tools = communicationTools
    ) { _ ->
        HomeServicesPrompts.collectIssueDetailsInstructions(agentInput<String>())
    }

    // Phase 2: select a slot — find availability and let the user pick
    val selectSlot by node<String, SlotSelectionOutcome> {
        // TODO Implement selectSlot task
        throw NotImplementedError("Use subgraphWithTask with communicationTools and findAvailableSlotTools, " +
                "use HomeServicesPrompts.selectSlotInstructions prompt")
    }

    // Phase 3: confirm appointment — review details with the customer before booking
    val confirmAppointment by node<String, ConfirmationOutcome> { state ->
        // TODO Implement confirmAppointment task
        throw NotImplementedError("Use subgraphWithTask with communicationTools, " +
                "use HomeServicesPrompts.confirmAppointmentInstructions prompt with the state: $state"
        )
    }

    // Phase 4: book appointment — all data is already collected, no LLM needed
    val bookAppointment by node<String, String> { _ ->
        val issueDetails = storage.getValue(issueDetailsKey)
        val slot = storage.getValue(selectedSlotKey)
        bookingProvider.bookAppointment(
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
        tools = communicationTools
    ) { previousResult ->
        HomeServicesPrompts.collectFeedbackInstructions(previousResult)
    }

    // Cancellation path: single LLM call to thank and close the conversation
    val handleCancellation by node<String, String> { _ ->
        llm.writeSession {
            appendPrompt { user(HomeServicesPrompts.handleCancellationInstructions) }
            requestLLM().textContent()
        }
    }

    // Emergency path: direct the user to emergency services and close
    val handleEmergency by node<String, String> { justification ->
        llm.writeSession {
            appendPrompt { user(HomeServicesPrompts.respondToEmergencyInstructions(justification)) }
            requestLLM().textContent()
        }
    }

    nodeStart then triageEmergency
    edge(triageEmergency forwardTo collectIssueDetails onCondition { it.status == TriageOutcome.proceed_with_scheduling })
    edge(triageEmergency forwardTo handleCancellation onCondition { it.status == TriageOutcome.cancelled } transformed { "Cancelled" })
    edge(triageEmergency forwardTo handleEmergency onCondition { it.status == TriageOutcome.emergency_detected } transformed { it.justification ?: "Emergency situation detected" })

    edge(collectIssueDetails forwardTo selectSlot onCondition { it.success() } transformed { "Issue details collected" })
    edge(collectIssueDetails forwardTo handleCancellation onCondition { !it.success() } transformed { "Cancelled" })

    edge(selectSlot forwardTo confirmAppointment onCondition { it.success() }  transformed { "Slot selected" })
    edge(selectSlot forwardTo handleCancellation onCondition { !it.success() } transformed { "Cancelled" })

//    TODO: Add edges from "confirmAppointment" to "selectSlot", "handleCancellation" and "bookAppointment" with relevant conditions (and transformations to String)
    edge(confirmAppointment forwardTo handleCancellation transformed { "Cancelled" })

    bookAppointment then collectFeedback then nodeFinish
    handleCancellation then nodeFinish
    handleEmergency then nodeFinish
}
