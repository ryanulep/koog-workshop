package org.example.project.domain.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.example.project.domain.shared.CharacterId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class AskQuestionRepository {

    private val _answers = MutableSharedFlow<Pair<CharacterId, String>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    suspend fun getQuestion(characterId: CharacterId, sessionId: String): AskQuestionRecord? = withContext(Dispatchers.IO) {
        AskQuestions.selectAll()
            .where { (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }
            .map {
                AskQuestionRecord(
                    characterId = CharacterId(it[AskQuestions.character].value),
                    sessionId = it[AskQuestions.sessionId],
                    question = it[AskQuestions.question],
                    answer = it[AskQuestions.answer],
                    isAsked = it[AskQuestions.isAsked]
                )
            }
            .singleOrNull()
    }

    suspend fun upsertQuestion(characterId: CharacterId, sessionId: String, question: String) = withContext(Dispatchers.IO) {
        val existing = getQuestion(characterId, sessionId)
        if (existing == null) {
            AskQuestions.insert {
                it[character] = characterId.value
                it[this.sessionId] = sessionId
                it[this.question] = question
                it[isAsked] = false
            }
        } else {
            AskQuestions.update({ (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }) {
                it[this.question] = question
            }
        }
    }

    suspend fun markAsAsked(characterId: CharacterId, sessionId: String) = withContext(Dispatchers.IO) {
        AskQuestions.update({ (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }) {
            it[isAsked] = true
        }
    }

    suspend fun answerQuestion(characterId: CharacterId, sessionId: String, answer: String) = withContext(Dispatchers.IO) {
        AskQuestions.update({ (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }) {
            it[this.answer] = answer
        }
        _answers.emit(characterId to sessionId)
    }

    fun subscribeToAnswer(characterId: CharacterId, sessionId: String): Flow<String> {
        return _answers
            .filter { it.first == characterId && it.second == sessionId }
            .map {
                getQuestion(characterId, sessionId)?.answer ?: ""
            }
            .filter { it.isNotEmpty() }
    }
}

data class AskQuestionRecord(
    val characterId: CharacterId,
    val sessionId: String,
    val question: String,
    val answer: String?,
    val isAsked: Boolean
)
