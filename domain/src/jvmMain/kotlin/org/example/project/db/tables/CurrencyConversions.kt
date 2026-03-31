package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CurrencyConversions : LongIdTable("currency_conversions") {
    val fromCurrency = reference("from_currency_id", Currencies)
    val toCurrency = reference("to_currency_id", Currencies)
    val rate = double("rate")

    init {
        uniqueIndex(fromCurrency, toCurrency)
    }
}
