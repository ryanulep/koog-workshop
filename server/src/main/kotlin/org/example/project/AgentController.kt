package org.example.project

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.ChatAgentProvider
import org.example.project.koog.tracking.AgentExecutionTraceEvent
import org.example.project.shared.ChatMessage
import org.example.project.shared.ExecutionTraceItem
import org.example.project.shared.LlmCallData
import org.example.project.shared.LlmCallHistoryItem
import org.example.project.shared.LlmCallToolData
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.uuid.Uuid

@Controller
class AgentController(
    private val provider: ChatAgentProvider,
    private val chat: ChatHistoryProvider,
) {
    private val sseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private fun SseEmitter.sendChatMessage(message: ChatMessage) {
        val data = json.encodeToString(ChatMessage.serializer(), message)
        send(data, MediaType.APPLICATION_JSON)
    }

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
            onToolCallEvent = { toolName, args ->
                emitter.sendChatMessage(ChatMessage.ToolCallMessage(toolName, args))
            },
            onLLMCallEvent = { messages, tools ->
                emitter.sendChatMessage(
                    ChatMessage.LLMCallMessage(
                        LlmCallData(
                            messageHistory = messages.toHistoryItems(),
                            availableTools = tools.toToolData()
                        )
                    )
                )
            },
            onErrorEvent = { error ->
                emitter.sendChatMessage(ChatMessage.ErrorMessage(error))
            },
            onExecutionTraceEvent = { trace ->
                emitter.sendChatMessage(
                    ChatMessage.ExecutionTraceMessage(
                        when (trace) {
                            is AgentExecutionTraceEvent.Node -> ExecutionTraceItem.Node(trace.name)
                            is AgentExecutionTraceEvent.Subgraph -> ExecutionTraceItem.Subgraph(trace.name)
                        }
                    )
                )
            },
            onAskMessage = { message ->
                emitter.sendChatMessage(ChatMessage.AskQuestion(message))
            }
        )
        sseScope.launch {
            try {
                val response = agent.run(input, sessionId)
                emitter.sendChatMessage(ChatMessage.AgentMessage(response))
                emitter.complete()
            } catch (e: Exception) {
                e.printStackTrace()
                emitter.sendChatMessage(ChatMessage.ErrorMessage(e.message ?: "Unknown error occurred"))
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
