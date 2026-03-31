package org.example.project.db.repository

import org.example.project.db.tables.Currencies
import org.example.project.db.tables.CurrencyConversions
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.model.Currency
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ExposedCurrencyRepository {

    context(_: Transaction)
    fun getAllCurrencies(): List<Currency> =
        Currencies.selectAll().map(::mapToCurrency)

    context(_: Transaction)
    fun getCurrencyOrNull(id: CurrencyId): Currency? =
        Currencies.selectAll().where { Currencies.id eq id.value }
            .map(::mapToCurrency)
            .singleOrNull()

    context(_: Transaction)
    fun getConversionRateOrNull(fromId: CurrencyId, toId: CurrencyId): Double? =
        CurrencyConversions.selectAll()
            .where { (CurrencyConversions.fromCurrency eq fromId.value) and (CurrencyConversions.toCurrency eq toId.value) }
            .map { it[CurrencyConversions.rate] }
            .singleOrNull()

    private fun mapToCurrency(row: ResultRow) = Currency(
        id = CurrencyId(row[Currencies.id].value),
        code = row[Currencies.code],
        name = row[Currencies.name],
        symbol = row[Currencies.symbol],
        iconPath = row[Currencies.iconPath],
        createdAt = row[Currencies.createdAt],
        updatedAt = row[Currencies.updatedAt],
    )
}
