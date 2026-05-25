@file:Suppress("EnumEntryName")

package com.jetbrains.koog.workshop.agents.homeservices.graph

import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.Concept
import ai.koog.agents.core.dsl.extension.FactType
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.ext.agent.subgraphWithTask
import com.jetbrains.koog.workshop.agents.homeservices.HomeServicesBookingProvider

fun homeServicesStrategy(
  communicationTools: List<Tool<*, *>>,
  findAvailableSlotTools: List<Tool<*, *>>,
  bookingProvider: HomeServicesBookingProvider,
) =
  strategy<String, String>("home-services-scheduling") {
    val issueDetailsKey = createStorageKey<IssueDetails>("issue-details")
    val selectedSlotKey = createStorageKey<SelectedSlot>("selected-slot")

    // Phase 0: triage — detect emergencies before any scheduling
    val triageEmergency by
      subgraphWithTask<String, TriageResult>(tools = communicationTools) { input ->
        HomeServicesPrompts.triageEmergencyInstructions(input)
      }

    // Phase 1: collect issue details from the user (no search or booking tools)
    val collectIssueDetails by
      subgraphWithTask<TriageResult, IssueDetailsOutcome>(tools = communicationTools) { _ ->
        HomeServicesPrompts.collectIssueDetailsInstructions(agentInput<String>())
      }

    // Phase 1a: compress the history so as not to take up so much of the context window
    val compressHistory by
    nodeLLMCompressHistory<IssueDetailsOutcome>(
      strategy =
        HistoryCompressionStrategy.FactRetrieval(
          Concept(
            keyword = "important-details",
            description = "What are the critically important details of the issue",
            factType = FactType.MULTIPLE,
          )
        )
    )

    // Phase 2: select a slot — find availability and let the user pick
    val selectSlot by
      subgraphWithTask<String, SlotSelectionOutcome>(
        tools = communicationTools + findAvailableSlotTools
      ) {
        val issueDetails = storage.getValue(issueDetailsKey)
        HomeServicesPrompts.selectSlotInstructions(issueDetails, it)
      }

    // Phase 3: confirm appointment — review details with the customer before booking
    val confirmAppointment by
      subgraphWithTask<String, ConfirmationOutcome>(tools = communicationTools) { state ->
        val issueDetails = storage.getValue(issueDetailsKey)
        val selectedSlot = storage.getValue(selectedSlotKey)
        HomeServicesPrompts.confirmAppointmentInstructions(issueDetails, selectedSlot, state)
      }

    // Phase 4: book appointment — all data is already collected, no LLM needed
    val bookAppointment by
      node<String, String> { _ ->
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
    val collectFeedback by
      subgraphWithTask<String, String>(tools = communicationTools) { previousResult ->
        HomeServicesPrompts.collectFeedbackInstructions(previousResult)
      }

    // Cancellation path: single LLM call to thank and close the conversation
    val handleCancellation by
      node<String, String> { _ ->
        llm.writeSession {
          appendPrompt { user(HomeServicesPrompts.handleCancellationInstructions) }
          requestLLM().textContent()
        }
      }

    // Emergency path: direct the user to emergency services and close
    val handleEmergency by
      node<String, String> { justification ->
        llm.writeSession {
          appendPrompt { user(HomeServicesPrompts.respondToEmergencyInstructions(justification)) }
          requestLLM().textContent()
        }
      }

    nodeStart then triageEmergency
    edge(
      triageEmergency forwardTo
        collectIssueDetails onCondition
        {
          it.status == TriageOutcome.proceed_with_scheduling
        }
    )
    edge(
      triageEmergency forwardTo
        handleCancellation onCondition
        {
          it.status == TriageOutcome.cancelled
        } transformed
        {
          "Cancelled"
        }
    )
    edge(
      triageEmergency forwardTo
        handleEmergency onCondition
        {
          it.status == TriageOutcome.emergency_detected
        } transformed
        {
          it.justification ?: "Emergency situation detected"
        }
    )

    edge(collectIssueDetails forwardTo compressHistory)

    edge(
      compressHistory forwardTo
        selectSlot onCondition
        {
          it.success()
        } transformed
        { outcome ->
          storage.set(issueDetailsKey, outcome.collected!!)
          "Issue details collected"
        }
    )
    edge(
      compressHistory forwardTo
        handleCancellation onCondition
        {
          !it.success()
        } transformed
        {
          "Cancelled"
        }
    )

    edge(
      selectSlot forwardTo
        confirmAppointment onCondition
        {
          it.success()
        } transformed
        { outcome ->
          storage.set(selectedSlotKey, outcome.selected!!)
          "Slot selected"
        }
    )
    edge(
      selectSlot forwardTo
        handleCancellation onCondition
        {
          !it.success()
        } transformed
        {
          "Cancelled"
        }
    )

    edge(
      confirmAppointment forwardTo
        handleCancellation onCondition
        { outcome ->
          outcome == ConfirmationOutcome.cancelled
        } transformed
        {
          "Cancelled"
        }
    )
    edge(
      confirmAppointment forwardTo
        selectSlot onCondition
        {
          it == ConfirmationOutcome.change_requested
        } transformed
        {
          "Slot change requested"
        }
    )
    edge(
      confirmAppointment forwardTo
        bookAppointment onCondition
        { outcome ->
          outcome == ConfirmationOutcome.confirmed
        } transformed
        {
          "Appointment slot confirmed"
        }
    )

    bookAppointment then collectFeedback then nodeFinish
    handleCancellation then nodeFinish
    handleEmergency then nodeFinish
  }
