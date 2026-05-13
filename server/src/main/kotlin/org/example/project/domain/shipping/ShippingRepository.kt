package org.example.project.domain.shipping

import org.example.project.db.findByIdOrNull
import org.example.project.db.update
import org.example.project.domain.shipping.MerchantShippingMethods
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import org.example.project.domain.shipping.ShippingMethod
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Service

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Service
class ShippingRepository {

    fun getAllShippingMethods(): List<ShippingMethod> =
        ShippingMethods.selectAll().map(::mapToShippingMethod)

    fun getShippingMethodByIdOrNull(id: ShippingMethodId): ShippingMethod? =
        ShippingMethods.findByIdOrNull(id.value, ::mapToShippingMethod)

    fun getShippingMethodsForMerchant(merchantId: MerchantId): List<ShippingMethod> =
        (ShippingMethods innerJoin MerchantShippingMethods)
            .selectAll()
            .where { MerchantShippingMethods.merchant eq merchantId.value }
            .map(::mapToShippingMethod)

    fun createShippingMethod(
        name: String,
        description: String? = null,
        baseCost: Long,
        currencyId: CurrencyId,
        estimatedDays: Int
    ): ShippingMethodId {
        require(baseCost >= 0) { "Shipping base cost must be non-negative" }
        return ShippingMethodId(
            ShippingMethods.insertAndGetId {
                it[ShippingMethods.name] = name
                it[ShippingMethods.description] = description
                it[ShippingMethods.baseCost] = baseCost
                it[currency] = currencyId.value
                it[ShippingMethods.estimatedDays] = estimatedDays
            }.value
        )
    }

    fun updateShippingMethod(
        id: ShippingMethodId,
        name: String? = null,
        description: String? = null,
        baseCost: Long? = null,
        currencyId: CurrencyId? = null,
        estimatedDays: Int? = null,
        isActive: Boolean? = null
    ): Boolean {
        require(baseCost == null || baseCost >= 0) { "Shipping base cost must be non-negative" }
        return ShippingMethods.update(id.value) {
            if (name != null) it[ShippingMethods.name] = name
            if (description != null) it[ShippingMethods.description] = description
            if (baseCost != null) it[ShippingMethods.baseCost] = baseCost
            if (currencyId != null) it[currency] = currencyId.value
            if (estimatedDays != null) it[ShippingMethods.estimatedDays] = estimatedDays
            if (isActive != null) it[ShippingMethods.isActive] = isActive
        } > 0
    }

    fun setShippingMethodActive(id: ShippingMethodId, isActive: Boolean): Boolean =
        ShippingMethods.update(id.value) {
            it[ShippingMethods.isActive] = isActive
        } > 0

    fun deleteShippingMethod(id: ShippingMethodId): Boolean {
        MerchantShippingMethods.deleteWhere { shippingMethod eq id.value }
        return ShippingMethods.deleteWhere { ShippingMethods.id eq id.value } > 0
    }

    fun addShippingMethodToMerchant(merchantId: MerchantId, shippingMethodId: ShippingMethodId): Boolean {
        MerchantShippingMethods.insert {
            it[merchant] = merchantId.value
            it[shippingMethod] = shippingMethodId.value
        }
        return true
    }

    fun removeShippingMethodFromMerchant(merchantId: MerchantId, shippingMethodId: ShippingMethodId): Boolean =
        MerchantShippingMethods.deleteWhere {
            (MerchantShippingMethods.merchant eq merchantId.value) and
            (MerchantShippingMethods.shippingMethod eq shippingMethodId.value)
        } > 0

    fun replaceMerchantShippingMethods(
        merchantId: MerchantId,
        shippingMethodIds: Set<ShippingMethodId>
    ) {
        MerchantShippingMethods.deleteWhere { merchant eq merchantId.value }
        shippingMethodIds.distinctBy { it.value }.forEach { shippingMethodId ->
            MerchantShippingMethods.insert {
                it[merchant] = merchantId.value
                it[shippingMethod] = shippingMethodId.value
            }
        }
    }

    private fun mapToShippingMethod(row: ResultRow) = ShippingMethod(
        id = ShippingMethodId(row[ShippingMethods.id].value),
        name = row[ShippingMethods.name],
        description = row[ShippingMethods.description],
        baseCost = row[ShippingMethods.baseCost],
        currencyId = CurrencyId(row[ShippingMethods.currency].value),
        estimatedDays = row[ShippingMethods.estimatedDays],
        isActive = row[ShippingMethods.isActive],
        createdAt = row[ShippingMethods.createdAt],
        updatedAt = row[ShippingMethods.updatedAt]
    )
}
