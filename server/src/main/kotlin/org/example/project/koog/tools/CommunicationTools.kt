package org.example.project.koog.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.domain.chat.AskQuestionRepository
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.tracking.sendChatMessage
import org.example.project.shared.ChatMessage
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class CommunicationTools(
    private val characterId: CharacterId,
    private val sessionId: String,
    private val repository: AskQuestionRepository,
    private val emitter: SseEmitter
) : ToolSet {
    @Tool
    @LLMDescription("Ask a question to customer of the Fantasy Store assistant.")
    suspend fun askQuestion(message: String): String = withContext(Dispatchers.IO) {
        repository.askQuestion(characterId, sessionId, message) { message ->
            emitter.sendChatMessage(ChatMessage.AskQuestion(message))
        }.await()
    }
}