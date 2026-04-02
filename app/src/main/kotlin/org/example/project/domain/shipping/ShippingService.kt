package org.example.project.domain.shipping

import org.example.project.domain.shipping.ShippingRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import org.example.project.domain.shipping.ShippingMethod
import org.jetbrains.exposed.v1.jdbc.Database

class ShippingService(
    private val database: Database,
    private val shippingRepository: ShippingRepository = ShippingRepository()
) {
    suspend fun getAllShippingMethods(): List<ShippingMethod> =
        database.suspendTransaction { shippingRepository.getAllShippingMethods() }

    suspend fun getShippingMethodOrNull(id: ShippingMethodId): ShippingMethod? =
        database.suspendTransaction { shippingRepository.getShippingMethodByIdOrNull(id) }

    suspend fun createShippingMethod(
        name: String,
        description: String? = null,
        baseCost: Long,
        currencyId: CurrencyId,
        estimatedDays: Int
    ): ShippingMethodId =
        database.suspendTransaction {
            shippingRepository.createShippingMethod(name, description, baseCost, currencyId, estimatedDays)
        }

    suspend fun updateShippingMethod(
        id: ShippingMethodId,
        name: String? = null,
        description: String? = null,
        baseCost: Long? = null,
        currencyId: CurrencyId? = null,
        estimatedDays: Int? = null,
        isActive: Boolean? = null
    ): Boolean =
        database.suspendTransaction {
            shippingRepository.updateShippingMethod(id, name, description, baseCost, currencyId, estimatedDays, isActive)
        }

    suspend fun deleteShippingMethod(id: ShippingMethodId): Boolean =
        database.suspendTransaction { shippingRepository.deleteShippingMethod(id) }

    suspend fun getMerchantShippingMethods(merchantId: MerchantId): List<ShippingMethod> =
        database.suspendTransaction { shippingRepository.getShippingMethodsForMerchant(merchantId) }

    suspend fun addShippingMethodToMerchant(
        merchantId: MerchantId,
        shippingMethodId: ShippingMethodId
    ): Boolean =
        database.suspendTransaction {
            shippingRepository.addShippingMethodToMerchant(merchantId, shippingMethodId)
        }

    suspend fun removeShippingMethodFromMerchant(
        merchantId: MerchantId,
        shippingMethodId: ShippingMethodId
    ): Boolean =
        database.suspendTransaction {
            shippingRepository.removeShippingMethodFromMerchant(merchantId, shippingMethodId)
        }
}
