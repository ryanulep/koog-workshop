package org.example.project.db.repository

import org.example.project.db.tables.MerchantShippingMethods
import org.example.project.db.tables.ShippingMethods
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.id.MerchantId
import org.example.project.domain.id.ShippingMethodId
import org.example.project.domain.model.ShippingMethod
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ShippingRepository {

    context(_: Transaction)
    fun getAllShippingMethods(): List<ShippingMethod> =
        ShippingMethods.selectAll().map(::mapToShippingMethod)

    context(_: Transaction)
    fun getShippingMethodByIdOrNull(id: ShippingMethodId): ShippingMethod? =
        ShippingMethods.selectAll().where { ShippingMethods.id eq id.value }
            .map(::mapToShippingMethod)
            .singleOrNull()

    context(_: Transaction)
    fun getShippingMethodsForMerchant(merchantId: MerchantId): List<ShippingMethod> =
        (ShippingMethods innerJoin MerchantShippingMethods)
            .selectAll()
            .where { MerchantShippingMethods.merchant eq merchantId.value }
            .map(::mapToShippingMethod)

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
