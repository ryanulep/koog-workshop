package org.example.project.koog

import ai.koog.agents.core.agent.context.DetachedPromptExecutorAPI
import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.CriticResult
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.agent.subgraphWithVerification
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.serialization.Serializable
import org.example.project.domain.order.OrderStatus
import org.example.project.koog.tools.CustomerSupportTools
import org.example.project.koog.tools.plus
import kotlin.time.Instant

@Serializable
data class OrderDetails(
    val orderId: String,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class IssueSolution(val actionsTaken: String)


// TODO: DELETE
@Serializable
public data class ModeratedMessage(val message: Message, val moderationResult: ModerationResult)

// FIXME I see a problem with how this strategy works with ChatMemory feature.
//  ChatMemory only persists messages in the agent's prompt when the strategy finishes.
//  Strategy outputs (like a String message in this case) are not persisted.
//  So if we want the strategy result to be some message, let's make the return type Message.Assistant
//  and make sure to add it to the agent's prompt at the end of the strategy, e.g. with a custom node.
@OptIn(DetachedPromptExecutorAPI::class)
fun orderCustomerSupportStrategy(tools: CustomerSupportTools) = strategy<String, String>("order-customer-support") {
    // TODO add context gathering custom node?

    val moderate by node<Message, ModeratedMessage> { message ->
        val moderationPrompt = prompt("single-message-moderation") { message(message) }
        val moderationResult = llm.promptExecutor.moderate(moderationPrompt, OpenAIModels.Moderation.Omni)
        ModeratedMessage(message, moderationResult)
    }
//    TODO:
//    nodeLLMModerateMessage(
//        moderatingModel = OpenAIModels.Moderation.Omni
//    )

    val summarize by subgraphWithTask<String, OrderDetails>(tools = tools.communicationTools + tools.readOrderTools) { input ->
        """
            $input
        """.trimIndent()
    }

    val resolve by subgraphWithTask<OrderDetails, IssueSolution>(tools = tools.readOrderTools + tools.updateOrderTools) { details ->
        """
            Resolve the problem ${agentInput<String>()} given the details: $details
            Tell us what actions you took to fix the problem when you're done.
        """.trimIndent()
    }

    val verify by subgraphWithVerification<IssueSolution>(tools = tools.readOrderTools + tools.communicationTools) { solution ->
        "Verify if $solution fixed the problem. Also verify its actually fixed by checking the system state if necessary."
    }

    val adjust by subgraphWithTask<CriticResult<IssueSolution>, IssueSolution>(tools = tools.readOrderTools + tools.updateOrderTools) { critique ->
        """
            We need to solve the user problem: ${agentInput<String>()}.
            Following steps didn't solve the problem: ${critique.input}
            These problems still presists: ${critique.feedback}
        """.trimIndent()
    }

    val describe by subgraphWithTask<IssueSolution, String>(tools = emptyList()) { solution ->
        """
            Provide a nice and friendly summarizing message for the user how we solved their problem.
            Problem: ${agentInput<String>()}
            Actions Taken: ${solution.actionsTaken}
        """.trimIndent()
    }

    edge(nodeStart forwardTo moderate transformed { Message.User(it, RequestMetaInfo.Empty) })
    edge(moderate forwardTo summarize onCondition { !it.moderationResult.isHarmful } transformed {
        // TODO: parts.textContent()
        it.message.parts.filterIsInstance<MessagePart.Text>().joinToString(separator = "\n") { it.text }
    })
    edge(
        moderate forwardTo nodeFinish
                onCondition { it.moderationResult.isHarmful }
                transformed { "We cannot help you with this kind of requests." }
    )
    summarize then resolve then verify
    edge(verify forwardTo describe onCondition { it.successful } transformed { it.input })
    edge(verify forwardTo adjust onCondition { !it.successful })
    adjust then verify
    describe then nodeFinish
}