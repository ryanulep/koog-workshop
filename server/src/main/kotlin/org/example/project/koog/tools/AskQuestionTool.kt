package org.example.project.koog.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.flow.first
import org.example.project.domain.chat.AskQuestionRepository
import org.example.project.domain.shared.CharacterId

// FIXME similar to "intro", the class name is confusing since it doesn't define a tool, but a set of tools.
//  Rename to smth like "CommunicationTools" or convert to a class-based tool.
class AskQuestionTool(
    private val characterId: CharacterId,
    private val sessionId: String,
    private val repository: AskQuestionRepository,
    private val onAskMessage: (String) -> Unit
) : ToolSet {
    @Tool
    @LLMDescription("Ask a question to customer of the Fantasy Store assistant.")
    suspend fun askQuestion(message: String): String {
        val existing = repository.getQuestion(characterId, sessionId)
        if (existing == null || existing.answer == null) {
            repository.upsertQuestion(characterId, sessionId, message)
            val updated = repository.getQuestion(characterId, sessionId)
            if (updated != null && !updated.isAsked) {
                onAskMessage(message) // message is send over SSE
                repository.markAsAsked(characterId, sessionId)
            }
        }

        val current = repository.getQuestion(characterId, sessionId)
        if (current?.answer != null) return current.answer

        // Subscribe and wait for answer
        return repository.subscribeToAnswer(characterId, sessionId).first()
    }
}