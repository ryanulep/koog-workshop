package org.example.project.domain.admin

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import org.example.project.domain.catalog.Merchant
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId

@Immutable
data class MerchantListItem(
    val id: MerchantId,
    val name: String,
    val location: String?,
    val isActive: Boolean,
    val productCount: Int,
    val recentOrderCount: Int
)

@Immutable
data class ShippingMethodAssignmentItem(
    val id: ShippingMethodId,
    val name: String,
    val description: String?,
    val baseCost: Long,
    val currencyCode: String,
    val estimatedDays: Int,
    val isActive: Boolean,
    val isAssigned: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Immutable
data class MerchantDetail(
    val merchant: Merchant,
    val productCount: Int,
    val recentOrderCount: Int,
    val assignedShippingMethods: List<ShippingMethodAssignmentItem>,
    val availableShippingMethods: List<ShippingMethodAssignmentItem>
)
