package org.example.project

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.features.persistence.jdbc.JdbcPersistenceStorageProvider
import ai.koog.prompt.message.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Controller
class AgentController(
    private val provider: ChatAgentProvider,
    private val chat: ChatHistoryProvider,
    private val persistence: JdbcPersistenceStorageProvider
) {
    private val logger = KotlinLogging.logger {}
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
        val emitter = SseEmitter(Long.MAX_VALUE)
        logger.info { "Creating SseEmitter for session: $sessionId, character: $characterId" }

        emitter.onCompletion {
            logger.info { "SseEmitter completed for session: $sessionId" }
        }
        emitter.onTimeout {
            logger.warn { "SseEmitter timed out for session: $sessionId" }
            emitter.complete()
        }
        emitter.onError {
            logger.error(it) { "SseEmitter error for session: $sessionId: ${it.message}" }
        }

        val agent = provider.provideAgent(
            characterId = CharacterId(Uuid.parse(characterId)),
            sessionId = sessionId,
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
            },
            persistence = persistence
        )

        val heartbeatJob = sseScope.launch {
            try {
                while (isActive) {
                    delay(15.seconds)
                    emitter.sendChatMessage(ChatMessage.Heartbeat)
                }
            } catch (e: Exception) {
                logger.debug { "Heartbeat failed for session $sessionId: ${e.message}" }
            }
        }

        sseScope.launch {
            try {
                logger.info { "Running agent for session: $sessionId" }
                val response = agent.run(input, sessionId)
                logger.info { "Agent finished for session: $sessionId" }
                emitter.sendChatMessage(ChatMessage.AgentMessage(response))
            } catch (e: Exception) {
                logger.error(e) { "Error running agent for session: $sessionId" }
                emitter.sendChatMessage(ChatMessage.ErrorMessage(e.message ?: "Unknown error occurred"))
            } finally {
                heartbeatJob.cancelAndJoin()
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
