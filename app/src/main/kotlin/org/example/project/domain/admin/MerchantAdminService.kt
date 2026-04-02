package org.example.project.domain.admin

import org.example.project.db.suspendTransaction
import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.shipping.ShippingRepository
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import org.jetbrains.exposed.v1.jdbc.Database

class MerchantAdminService(
    private val database: Database,
    private val adminMerchantRepository: AdminMerchantRepository = AdminMerchantRepository(),
    private val merchantRepository: MerchantRepository = MerchantRepository(),
    private val shippingRepository: ShippingRepository = ShippingRepository()
) {
    suspend fun loadMerchants(): List<MerchantListItem> =
        database.suspendTransaction {
            adminMerchantRepository.getMerchants()
        }

    suspend fun loadMerchantDetailOrNull(merchantId: MerchantId): MerchantDetail? =
        database.suspendTransaction {
            adminMerchantRepository.getMerchantDetailOrNull(merchantId)
        }

    suspend fun setMerchantActive(merchantId: MerchantId, isActive: Boolean): Boolean =
        database.suspendTransaction {
            merchantRepository.setMerchantActive(merchantId, isActive)
        }

    suspend fun setShippingMethodActive(shippingMethodId: ShippingMethodId, isActive: Boolean): Boolean =
        database.suspendTransaction {
            shippingRepository.setShippingMethodActive(shippingMethodId, isActive)
        }

    suspend fun replaceMerchantShippingMethods(
        merchantId: MerchantId,
        shippingMethodIds: Set<ShippingMethodId>
    ): Boolean = database.suspendTransaction {
        if (merchantRepository.getMerchantOrNull(merchantId) == null) {
            return@suspendTransaction false
        }

        shippingRepository.replaceMerchantShippingMethods(merchantId, shippingMethodIds)
        true
    }
}
