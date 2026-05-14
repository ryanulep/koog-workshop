package org.example.project.domain.catalog

import kotlin.time.Instant
import org.example.project.domain.shared.MerchantId

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
