package com.jetbrains.example.koog.compose.agents.homeservices

import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.ext.agent.subgraphWithTask
import com.jetbrains.example.koog.compose.agents.common.AskUserTool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class EmergencyCheckResult {
    EMERGENCY_ACKNOWLEDGED,
    PROCEED_WITH_SCHEDULING,
}

@Serializable
data class IntakeResult(
    val serviceType: ServiceType,
    val issueSummary: String,
    val address: String,
    val customerName: String,
    val accessNotes: String? = null,
)

@Serializable
sealed class AssessResult {
    @Serializable @SerialName("cancelled")
    data object Cancelled : AssessResult()

    @Serializable @SerialName("collected")
    data class Collected(val details: IntakeResult) : AssessResult()
}

@Serializable
data class SelectedSlot(
    val slotId: String,
    val date: String,
    val timeWindow: String,
    val intake: IntakeResult,
)

@Serializable
sealed class ConfirmResult {
    @Serializable @SerialName("confirmed")
    data class Confirmed(val slot: SelectedSlot) : ConfirmResult()

    @Serializable @SerialName("change_requested")
    data class ChangeRequested(val intake: IntakeResult) : ConfirmResult()

    @Serializable @SerialName("cancelled")
    data object Cancelled : ConfirmResult()
}

fun homeServicesSchedulingStrategy(
    askUserTool: AskUserTool,
    findTools: HomeServicesFindTools,
    bookTools: HomeServicesBookTools,
) = strategy<String, String>("home-services-scheduling") {
    // FIXME Let's try non String inputs/outputs in some of these subtasks, to showcase domain modeling approach which is one of the Koog's strengths
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

    val compressHistory by nodeLLMCompressHistory<IntakeResult>()

    // Phase 2: find slots and let the user pick one (no booking tool available)
    val selectSlot by subgraphWithTask<IntakeResult, SelectedSlot>(
        tools = askUserTool.asTools() + findTools.asTools()
    ) { intake ->
        """
        $homeServicesSlotSelectionInstructions

        Intake results:
        - Customer: ${intake.customerName}
        - Service type: ${intake.serviceType}
        - Issue: ${intake.issueSummary}
        - Address: ${intake.address}
        ${intake.accessNotes?.let { "- Access notes: $it" } ?: ""}
        """.trimIndent()
    }

    // Phase 3: confirm the chosen date and time (only askUser — no tools to find or book)
    val confirmSlot by subgraphWithTask<SelectedSlot, ConfirmResult>(
        tools = askUserTool.asTools()
    ) { slot ->
        """
        $homeServicesConfirmationInstructions

        Selected slot:
        - Date: ${slot.date}
        - Time window: ${slot.timeWindow}
        - Service type: ${slot.intake.serviceType}
        - Customer: ${slot.intake.customerName}
        - Address: ${slot.intake.address}
        ${slot.intake.accessNotes?.let { "- Notes: $it" } ?: ""}

        Slot JSON (use this verbatim as the "slot" value when returning the result):
        ${Json.encodeToString(slot)}
        """.trimIndent()
    }

    // Phase 4: book the appointment (booking tool now available)
    val book by subgraphWithTask<SelectedSlot, String>(
        tools = askUserTool.asTools() + bookTools.asTools()
    ) { slot ->
        """
        $homeServicesBookingInstructions

        Confirmed booking details:
        - Customer: ${slot.intake.customerName}
        - Service type: ${slot.intake.serviceType}
        - Issue: ${slot.intake.issueSummary}
        - Slot ID: ${slot.slotId}
        - Date: ${slot.date}
        - Time window: ${slot.timeWindow}
        - Address: ${slot.intake.address}
        ${slot.intake.accessNotes?.let { "- Notes: $it" } ?: ""}
        """.trimIndent()
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

    nodeStart then checkEmergency
    edge(checkEmergency forwardTo nodeFinish onCondition { it == EmergencyCheckResult.EMERGENCY_ACKNOWLEDGED } transformed { "Handling emergency" })
    edge(checkEmergency forwardTo assess onCondition { it == EmergencyCheckResult.PROCEED_WITH_SCHEDULING })

    edge(assess forwardTo finish onCondition { it is AssessResult.Cancelled } transformed { "cancelled" })
    edge(assess forwardTo compressHistory onCondition { it is AssessResult.Collected } transformed { (it as AssessResult.Collected).details })

    compressHistory then selectSlot then confirmSlot

    edge(confirmSlot forwardTo selectSlot onCondition { it is ConfirmResult.ChangeRequested } transformed { (it as ConfirmResult.ChangeRequested).intake })
    edge(confirmSlot forwardTo finish onCondition { it is ConfirmResult.Cancelled } transformed { "cancelled" })
    edge(confirmSlot forwardTo book onCondition { it is ConfirmResult.Confirmed } transformed { (it as ConfirmResult.Confirmed).slot })

    book then finish then nodeFinish
}
