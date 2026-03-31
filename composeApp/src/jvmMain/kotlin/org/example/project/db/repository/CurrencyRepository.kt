package org.example.project.db.repository

import org.example.project.db.DatabaseFactory.dbQuery
import org.example.project.db.tables.Currencies
import org.example.project.db.tables.CurrencyConversions
import org.example.project.domain.model.Currency
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class CurrencyRepository {

    suspend fun getAllCurrencies(): List<Currency> = dbQuery {
        Currencies.selectAll().map(::mapToCurrency)
    }

    suspend fun getCurrencyById(id: Long): Currency? = dbQuery {
        Currencies.selectAll().where { Currencies.id eq id }
            .map(::mapToCurrency)
            .singleOrNull()
    }

    suspend fun getConversionRate(fromId: Long, toId: Long): Double? = dbQuery {
        CurrencyConversions.selectAll()
            .where { (CurrencyConversions.fromCurrency eq fromId) and (CurrencyConversions.toCurrency eq toId) }
            .map { it[CurrencyConversions.rate] }
            .singleOrNull()
    }

    private fun mapToCurrency(row: ResultRow) = Currency(
        id = row[Currencies.id].value,
        code = row[Currencies.code],
        name = row[Currencies.name],
        symbol = row[Currencies.symbol],
        iconPath = row[Currencies.iconPath]
    )
}
