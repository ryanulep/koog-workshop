package org.example.project.domain.currency

import org.example.project.domain.currency.Currencies
import org.example.project.domain.currency.CurrencyConversions
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.currency.Currency
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.example.project.domain.shared.CurrencyConversionId
import org.example.project.domain.currency.CurrencyConversion
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.example.project.db.update as storeUpdate

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class CurrencyRepository {

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

    context(_: Transaction)
    fun createCurrency(code: String, name: String, symbol: String, iconPath: String? = null): CurrencyId =
        CurrencyId(
            Currencies.insertAndGetId {
                it[Currencies.code] = code
                it[Currencies.name] = name
                it[Currencies.symbol] = symbol
                it[Currencies.iconPath] = iconPath
            }.value
        )

    context(_: Transaction)
    fun updateCurrency(
        id: CurrencyId,
        code: String? = null,
        name: String? = null,
        symbol: String? = null,
        iconPath: String? = null
    ): Boolean =
        Currencies.storeUpdate({ Currencies.id eq id.value }) {
            if (code != null) it[Currencies.code] = code
            if (name != null) it[Currencies.name] = name
            if (symbol != null) it[Currencies.symbol] = symbol
            if (iconPath != null) it[Currencies.iconPath] = iconPath
        } > 0

    context(_: Transaction)
    fun deleteCurrency(id: CurrencyId): Boolean =
        Currencies.deleteWhere { Currencies.id eq id.value } > 0

    context(_: Transaction)
    fun createConversionRate(fromId: CurrencyId, toId: CurrencyId, rate: Double): CurrencyConversionId =
        CurrencyConversionId(
            CurrencyConversions.insertAndGetId {
                it[fromCurrency] = fromId.value
                it[toCurrency] = toId.value
                it[CurrencyConversions.rate] = rate
            }.value
        )

    context(_: Transaction)
    fun updateConversionRate(id: CurrencyConversionId, rate: Double): Boolean =
        CurrencyConversions.storeUpdate({ CurrencyConversions.id eq id.value }) {
            it[CurrencyConversions.rate] = rate
        } > 0

    context(_: Transaction)
    fun deleteConversionRate(id: CurrencyConversionId): Boolean =
        CurrencyConversions.deleteWhere { CurrencyConversions.id eq id.value } > 0

    context(_: Transaction)
    fun getAllConversionRates(): List<CurrencyConversion> =
        CurrencyConversions.selectAll().map(::mapToConversion)

    private fun mapToCurrency(row: ResultRow) = Currency(
        id = CurrencyId(row[Currencies.id].value),
        code = row[Currencies.code],
        name = row[Currencies.name],
        symbol = row[Currencies.symbol],
        iconPath = row[Currencies.iconPath],
        createdAt = row[Currencies.createdAt],
        updatedAt = row[Currencies.updatedAt],
    )

    private fun mapToConversion(row: ResultRow) = CurrencyConversion(
        id = CurrencyConversionId(row[CurrencyConversions.id].value),
        fromCurrencyId = CurrencyId(row[CurrencyConversions.fromCurrency].value),
        toCurrencyId = CurrencyId(row[CurrencyConversions.toCurrency].value),
        rate = row[CurrencyConversions.rate],
        createdAt = row[CurrencyConversions.createdAt],
        updatedAt = row[CurrencyConversions.updatedAt]
    )
}
