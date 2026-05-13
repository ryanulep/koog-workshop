package org.example.project.domain.character

import org.example.project.domain.shared.CharacterId
import kotlin.time.Instant

data class Character(
    val id: CharacterId,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
