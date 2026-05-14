package org.example.project.domain.chat

import ai.koog.prompt.message.Message
import kotlinx.serialization.Serializable
import org.example.project.domain.shared.CharacterId

@Serializable
data class ChatDetails(
    val characterId: CharacterId,
    val conversationId: String,
    val messages: List<Message>
)

@Serializable
data class ChatUpdate(
    val characterId: CharacterId,
    val conversationId: String,
)
