package org.example.project.admin.merchants

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.example.project.domain.admin.MerchantDetail
import org.example.project.domain.admin.MerchantListItem
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId

@Immutable
data class MerchantAdminUiState(
    val errorMessage: String? = null,
    val merchants: PersistentList<org.example.project.domain.admin.MerchantListItem> = persistentListOf(),
    val selectedMerchantId: org.example.project.domain.shared.MerchantId? = null,
    val selectedMerchant: org.example.project.domain.admin.MerchantDetail? = null,
    val selectedShippingMethodIds: PersistentSet<org.example.project.domain.shared.ShippingMethodId> = persistentSetOf()
) {
    val hasPendingShippingAssignments: Boolean
        get() = selectedMerchant != null &&
            selectedMerchant.assignedShippingMethods.map { shippingMethod -> shippingMethod.id }.toSet() !=
            selectedShippingMethodIds.toSet()
}
