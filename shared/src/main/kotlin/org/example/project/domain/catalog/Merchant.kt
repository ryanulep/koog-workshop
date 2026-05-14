package org.example.project.domain.catalog

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import org.example.project.domain.shared.MerchantId

@Serializable
data class Merchant(
    val id: MerchantId,
    val name: String,
    val description: String?,
    val location: String?,
    val theme: String?,
    val iconPath: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
