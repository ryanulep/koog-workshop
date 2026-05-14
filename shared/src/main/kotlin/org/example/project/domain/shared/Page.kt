package org.example.project.domain.shared

import kotlinx.serialization.Serializable

@Serializable
data class Page<T>(
    val items: List<T>,
    val total: Long,
    val offset: Long,
    val limit: Long
)
