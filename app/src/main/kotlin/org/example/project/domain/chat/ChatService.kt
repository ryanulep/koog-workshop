package org.example.project.domain.chat

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import org.example.project.db.suspendTransaction
import org.example.project.domain.shared.CharacterId
import org.jetbrains.exposed.v1.jdbc.Database

class ChatService(
    private val database: Database,
    private val chatHistoryProvider: ChatHistoryProvider,
    private val chatRepository: ChatRepository = ChatRepository(),
) {
    /**
     * Fetch all chats for the current user with the chat history
     */
    suspend fun getCharacterChatDetails(characterId: CharacterId): List<ChatDetails> {
        val chats = database.suspendTransaction {
            chatRepository.getCharacterChats(characterId)
        }

        return chats.map {
            ChatDetails(
                characterId = it.characterId,
                conversationId = it.conversationId,
                messages = chatHistoryProvider.load(it.conversationId)
            )
        }
    }

    /**
     * Create new character <-> conversation relationship or notify that it was updated (e.g. new messages)
     */
    suspend fun updateChat(update: ChatUpdate) = database.suspendTransaction {
        chatRepository.updateChat(update)
    }
}