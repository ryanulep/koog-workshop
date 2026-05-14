package org.example.project.domain.chat

import org.example.project.domain.shared.CharacterId
import kotlin.time.Instant
import kotlinx.serialization.Serializable

data class Chat(
    val characterId: CharacterId,
    val conversationId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)