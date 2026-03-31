package org.example.project.db.repository

import org.example.project.db.suspendTransaction
import org.example.project.db.tables.MerchantShippingMethods
import org.example.project.db.tables.Merchants
import org.example.project.db.tables.ShippingMethods
import org.example.project.domain.model.Merchant
import org.example.project.domain.model.ShippingMethod
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll

class MerchantRepository(
    private val database: Database
) {

    suspend fun getAllMerchants(): List<Merchant> = database.suspendTransaction {
        Merchants.selectAll().map(::mapToMerchant)
    }

    suspend fun getMerchantById(id: Long): Merchant? = database.suspendTransaction {
        Merchants.selectAll().where { Merchants.id eq id }
            .map(::mapToMerchant)
            .singleOrNull()
    }

    suspend fun getShippingMethodsForMerchant(merchantId: Long): List<ShippingMethod> = database.suspendTransaction {
        (ShippingMethods innerJoin MerchantShippingMethods)
            .selectAll()
            .where { MerchantShippingMethods.merchant eq merchantId }
            .map(::mapToShippingMethod)
    }

    private fun mapToMerchant(row: ResultRow) = Merchant(
        id = row[Merchants.id].value,
        name = row[Merchants.name],
        description = row[Merchants.description],
        location = row[Merchants.location],
        theme = row[Merchants.theme],
        iconPath = row[Merchants.iconPath],
        isActive = row[Merchants.isActive]
    )

    private fun mapToShippingMethod(row: ResultRow) = ShippingMethod(
        id = row[ShippingMethods.id].value,
        name = row[ShippingMethods.name],
        description = row[ShippingMethods.description],
        baseCost = row[ShippingMethods.baseCost],
        currencyId = row[ShippingMethods.currency].value,
        estimatedDays = row[ShippingMethods.estimatedDays],
        isActive = row[ShippingMethods.isActive]
    )
}
