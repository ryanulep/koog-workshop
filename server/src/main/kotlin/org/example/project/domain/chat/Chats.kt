package org.example.project.domain.chat

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters

/**
 * Many-to-many relations between characters and their agent chat conversations
 */
object Chats : StoreTable("chats") {
    val character = reference("character_id", Characters)

    /**
     * FK to conversation from Koog-managed chat history table
     * @see org.example.project.koog.JdbcChatHistoryProvider
     */
    val conversationId = varchar("conversation_id", 255)

    init {
        uniqueIndex(character, conversationId)
    }
}