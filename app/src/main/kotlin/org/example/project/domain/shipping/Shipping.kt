package org.example.project.domain.shipping

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.ShippingMethodId

@Immutable
data class ShippingMethod(
    val id: ShippingMethodId,
    val name: String,
    val description: String?,
    val baseCost: Long,
    val currencyId: CurrencyId,
    val estimatedDays: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
