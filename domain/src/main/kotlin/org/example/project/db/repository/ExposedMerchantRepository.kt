package org.example.project.db.repository

import org.example.project.db.tables.MerchantShippingMethods
import org.example.project.db.tables.Merchants
import org.example.project.db.tables.ShippingMethods
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.id.MerchantId
import org.example.project.domain.id.ShippingMethodId
import org.example.project.domain.model.Merchant
import org.example.project.domain.model.ShippingMethod
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ExposedMerchantRepository {

    context(_: Transaction)
    fun getAllMerchants(): List<Merchant> =
        Merchants.selectAll().map(::mapToMerchant)

    context(_: Transaction)
    fun getMerchantOrNull(id: MerchantId): Merchant? =
        Merchants.selectAll().where { Merchants.id eq id.value }
            .map(::mapToMerchant)
            .singleOrNull()

    context(_: Transaction)
    fun getShippingMethodsForMerchant(merchantId: MerchantId): List<ShippingMethod> =
        (ShippingMethods innerJoin MerchantShippingMethods)
            .selectAll()
            .where { MerchantShippingMethods.merchant eq merchantId.value }
            .map(::mapToShippingMethod)

    private fun mapToMerchant(row: ResultRow) = Merchant(
        id = MerchantId(row[Merchants.id].value),
        name = row[Merchants.name],
        description = row[Merchants.description],
        location = row[Merchants.location],
        theme = row[Merchants.theme],
        iconPath = row[Merchants.iconPath],
        isActive = row[Merchants.isActive],
        createdAt = row[Merchants.createdAt],
        updatedAt = row[Merchants.updatedAt]
    )

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
