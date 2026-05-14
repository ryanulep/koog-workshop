package org.example.project.domain.chat

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters

object AskQuestions : StoreTable("ask_questions") {
    val character = reference("character_id", Characters)
    val sessionId = varchar("session_id", 255)
    val question = text("question")
    val answer = text("answer").nullable()
    val isAsked = bool("is_asked").default(false)

    init {
        uniqueIndex(character, sessionId)
    }
}
