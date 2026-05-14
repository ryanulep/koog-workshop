package org.example.project.domain.shipping

import org.example.project.domain.shipping.ShippingRepository

import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import org.example.project.domain.shipping.ShippingMethod
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class ShippingService(
    private val shippingRepository: ShippingRepository
) {
    fun getAllShippingMethods(): List<ShippingMethod> =
        shippingRepository.getAllShippingMethods()

    fun getShippingMethodOrNull(id: ShippingMethodId): ShippingMethod? =
         shippingRepository.getShippingMethodByIdOrNull(id)

    fun createShippingMethod(
        name: String,
        description: String? = null,
        baseCost: Long,
        currencyId: CurrencyId,
        estimatedDays: Int
    ): ShippingMethodId =
            shippingRepository.createShippingMethod(name, description, baseCost, currencyId, estimatedDays)

    fun updateShippingMethod(
        id: ShippingMethodId,
        name: String? = null,
        description: String? = null,
        baseCost: Long? = null,
        currencyId: CurrencyId? = null,
        estimatedDays: Int? = null,
        isActive: Boolean? = null
    ): Boolean =
            shippingRepository.updateShippingMethod(
                id,
                name,
                description,
                baseCost,
                currencyId,
                estimatedDays,
                isActive
            )

    fun deleteShippingMethod(id: ShippingMethodId): Boolean =
        shippingRepository.deleteShippingMethod(id)

    fun getMerchantShippingMethods(merchantId: MerchantId): List<ShippingMethod> =
        shippingRepository.getShippingMethodsForMerchant(merchantId)

    fun addShippingMethodToMerchant(
        merchantId: MerchantId,
        shippingMethodId: ShippingMethodId
    ): Boolean =
        shippingRepository.addShippingMethodToMerchant(merchantId, shippingMethodId)

    fun removeShippingMethodFromMerchant(
        merchantId: MerchantId,
        shippingMethodId: ShippingMethodId
    ): Boolean =
            shippingRepository.removeShippingMethodFromMerchant(merchantId, shippingMethodId)
}
