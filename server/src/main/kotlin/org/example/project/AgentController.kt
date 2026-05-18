package org.example.project

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.example.project.domain.chat.AskQuestionRepository
import org.example.project.domain.chat.ChatService
import org.example.project.domain.chat.ChatUpdate
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.ChatAgentProvider
import org.example.project.koog.sse
import org.example.project.koog.tools.CommunicationTools
import org.example.project.koog.tracking.SseEmitterEventHandler
import org.example.project.koog.tracking.sendChatMessage
import org.example.project.shared.AgentState
import org.example.project.shared.ChatMessage
import org.example.project.shared.LlmCallHistoryItem
import org.example.project.shared.LlmCallToolData
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.uuid.Uuid

@Controller
class AgentController(
    private val provider: ChatAgentProvider,
    private val askQuestionRepository: AskQuestionRepository,
    private val chatService: ChatService
) {
    private val logger = KotlinLogging.logger {}
    private val sseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @OptIn(ExperimentalAtomicApi::class)
    @PostMapping("chat")
    fun chat(
        @RequestParam characterId: String,
        @RequestParam input: String?,
        @RequestParam sessionId: String,
    ): SseEmitter = sseScope.sse { emitter ->
        val charId = CharacterId(Uuid.parse(characterId))
        chatService.updateChat(ChatUpdate(charId, sessionId))

        val agent = provider.provideAgent(
            characterId = charId,
            sseEventHandler = SseEmitterEventHandler(emitter),
            communicationTools = CommunicationTools(charId, sessionId, askQuestionRepository, emitter),
        )

        try {
            val response = agent.run(input ?: "", sessionId)
            emitter.sendChatMessage(ChatMessage.AgentMessage(response))
        } catch (e: Exception) {
            emitter.sendChatMessage(ChatMessage.ErrorMessage(e.message ?: "Unknown error occurred"))
        }
    }

    @PostMapping("chat/answer")
    suspend fun answerQuestion(
        @RequestParam characterId: String,
        @RequestParam sessionId: String,
        @RequestParam answer: String
    ) {
        val charId = CharacterId(Uuid.parse(characterId))
        logger.info { "Answering question for session: $sessionId, character: $characterId" }
        chatService.answerQuestion(charId, sessionId, answer)
    }

    @ResponseBody
    @GetMapping("chat/state")
    fun getAgentState(@RequestParam sessionId: String): AgentState = chatService.getAgentState(sessionId)
}

fun List<Message>.toHistoryItems(): List<LlmCallHistoryItem> =
    flatMap { message ->
        when (message) {
            is Message.System -> message.parts.map { LlmCallHistoryItem.System(it.text) }
            is Message.User -> message.parts.mapNotNull { part ->
                when (part) {
                    is MessagePart.Text -> LlmCallHistoryItem.User(part.text)
                    is MessagePart.Tool.Result -> LlmCallHistoryItem.ToolResult(part.tool, part.output)
                    is MessagePart.Attachment -> null
                }
            }

            is Message.Assistant -> message.parts.mapNotNull { part ->
                when (part) {
                    is MessagePart.Text -> LlmCallHistoryItem.Assistant(part.text)
                    is MessagePart.Reasoning -> LlmCallHistoryItem.Reasoning(part.content.joinToString(""))
                    is MessagePart.Tool.Call -> LlmCallHistoryItem.ToolCall(part.tool, part.args)
                    else -> null
                }
            }
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
