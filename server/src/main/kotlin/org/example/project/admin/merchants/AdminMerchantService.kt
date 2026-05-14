package org.example.project.admin.merchants


import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.shipping.ShippingRepository
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class AdminMerchantService(
    private val adminMerchantRepository: AdminMerchantRepository,
    private val merchantRepository: MerchantRepository,
    private val shippingRepository: ShippingRepository
) {
    suspend fun loadMerchants(): List<MerchantListItem> =
            adminMerchantRepository.getMerchants()

    suspend fun loadMerchantDetailOrNull(merchantId: MerchantId): MerchantDetail? =
            adminMerchantRepository.getMerchantDetailOrNull(merchantId)

    suspend fun setMerchantActive(merchantId: MerchantId, isActive: Boolean): Boolean =
            merchantRepository.setMerchantActive(merchantId, isActive)

    suspend fun setShippingMethodActive(shippingMethodId: ShippingMethodId, isActive: Boolean): Boolean =
            shippingRepository.setShippingMethodActive(shippingMethodId, isActive)

    suspend fun replaceMerchantShippingMethods(
        merchantId: MerchantId,
        shippingMethodIds: Set<ShippingMethodId>
    ): Boolean {
        if (merchantRepository.getMerchantOrNull(merchantId) == null) {
            return false
        }

        shippingRepository.replaceMerchantShippingMethods(merchantId, shippingMethodIds)
        return true
    }
}
