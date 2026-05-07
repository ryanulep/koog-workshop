package org.example.project.domain.chat

import ai.koog.prompt.message.Message
import androidx.compose.runtime.Immutable
import org.example.project.domain.shared.CharacterId
import kotlin.time.Instant

@Immutable
data class Chat(
    val characterId: CharacterId,
    val conversationId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Immutable
data class ChatDetails(
    val characterId: CharacterId,
    val conversationId: String,
    val messages: List<Message>
)

@Immutable
data class ChatUpdate(
    val characterId: CharacterId,
    val conversationId: String,
)