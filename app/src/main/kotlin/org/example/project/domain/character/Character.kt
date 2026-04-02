package org.example.project.domain.character

import androidx.compose.runtime.Immutable
import org.example.project.domain.shared.CharacterId
import kotlin.time.Instant

@Immutable
data class Character(
    val id: CharacterId,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
