package org.example.project

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.ChatAgentProvider
import org.example.project.koog.tracking.AgentExecutionTraceEvent
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.uuid.Uuid

@Serializable
sealed class ChatMessage {
    @Serializable
    data class UserMessage(val text: String) : ChatMessage()

    @Serializable
    data class AskQuestion(val text: String) : ChatMessage()

    @Serializable
    data class AgentMessage(val text: String) : ChatMessage()

    @Serializable
    data class SystemMessage(val text: String) : ChatMessage()

    @Serializable
    data class ErrorMessage(val text: String) : ChatMessage()

    @Serializable
    data class ToolCallMessage(val toolName: String, val args: Map<String, String>) : ChatMessage()

    @Serializable
    data class LLMCallMessage(val data: LlmCallData) : ChatMessage()

    @Serializable
    data class ExecutionTraceMessage(val item: ExecutionTraceItem) : ChatMessage()
}


@Serializable
sealed interface ExecutionTraceItem {
    val name: String

    data class Node(override val name: String) : ExecutionTraceItem
    data class Subgraph(override val name: String) : ExecutionTraceItem
}

@Serializable
data class LlmCallData(
    val messageHistory: List<LlmCallHistoryItem>,
    val availableTools: List<LlmCallToolData>,
)

@Serializable
sealed interface LlmCallHistoryItem {
    val text: String

    @Serializable
    data class System(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class User(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class Assistant(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class Reasoning(override val text: String) : LlmCallHistoryItem

    @Serializable
    data class ToolCall(val toolName: String, override val text: String) : LlmCallHistoryItem

    @Serializable
    data class ToolResult(val toolName: String, override val text: String) : LlmCallHistoryItem
}

@Serializable
data class LlmCallToolData(
    val name: String,
    val requiredParameters: List<String>,
    val optionalParameters: List<String>,
)

@Controller
class AgentController(
    private val provider: ChatAgentProvider,
    private val chat: ChatHistoryProvider,
) {
    private val sseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @PostMapping("chat")
    fun chat(
        @RequestParam characterId: String,
        @RequestParam input: String,
        @RequestParam sessionId: String,
    ): SseEmitter {
        val emitter = SseEmitter()
        val agent = provider.provideAgent(
            characterId = CharacterId(Uuid.parse(characterId)),
            historyProvider = chat,
            onToolCallEvent = { toolName, args -> emitter.send(ChatMessage.ToolCallMessage(toolName, args)) },
            onLLMCallEvent = { messages, tools ->
                ChatMessage.LLMCallMessage(
                    LlmCallData(
                        messageHistory = messages.toHistoryItems(),
                        availableTools = tools.toToolData()
                    )
                )
            },
            onErrorEvent = { error -> emitter.send(ChatMessage.ErrorMessage(error)) },
            onExecutionTraceEvent = { trace ->
                val item = when (trace) {
                    is AgentExecutionTraceEvent.Node -> ExecutionTraceItem.Node(trace.name)
                    is AgentExecutionTraceEvent.Subgraph -> ExecutionTraceItem.Subgraph(trace.name)
                }
                ChatMessage.ExecutionTraceMessage(item)
            },
            onAskMessage = { message -> emitter.send(ChatMessage.AskQuestion(message)) }
        )
        sseScope.launch {
            try {
                val response = agent.run(input, sessionId)
                emitter.send(ChatMessage.AgentMessage(response))
                emitter.complete()
            } catch (e: Exception) {
                emitter.send(ChatMessage.ErrorMessage(e.message ?: "Unknown error occurred"))
                emitter.complete()
            }
        }
        return emitter
    }
}

fun List<Message>.toHistoryItems(): List<LlmCallHistoryItem> =
    map { message ->
        when (message) {
            is Message.System -> LlmCallHistoryItem.System(message.content)
            is Message.User -> LlmCallHistoryItem.User(message.content)
            is Message.Assistant -> LlmCallHistoryItem.Assistant(message.content)
            is Message.Reasoning -> LlmCallHistoryItem.Reasoning(message.content)
            is Message.Tool.Call -> LlmCallHistoryItem.ToolCall(message.tool, message.content)
            is Message.Tool.Result -> LlmCallHistoryItem.ToolResult(message.tool, message.content)
        }
    }

fun List<ToolDescriptor>.toToolData(): List<LlmCallToolData> =
    map { tool ->
        LlmCallToolData(
            name = tool.name,
            requiredParameters = tool.requiredParameters.map { it.name },
            optionalParameters = tool.optionalParameters.map { it.name }
        )
    }
