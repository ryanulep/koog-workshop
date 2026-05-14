package org.example.project.domain.chat

import ai.koog.prompt.message.Message
import org.example.project.domain.shared.CharacterId
import kotlin.time.Instant

data class Chat(
    val characterId: CharacterId,
    val conversationId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ChatDetails(
    val characterId: CharacterId,
    val conversationId: String,
    val messages: List<Message>
)

data class ChatUpdate(
    val characterId: CharacterId,
    val conversationId: String,
)