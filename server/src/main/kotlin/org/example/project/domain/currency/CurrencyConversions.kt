package org.example.project.domain.currency

import org.example.project.db.StoreTable

object CurrencyConversions : StoreTable("currency_conversions") {
    val fromCurrency = reference("from_currency_id", Currencies)
    val toCurrency = reference("to_currency_id", Currencies)
    val rate = double("rate")

    init {
        uniqueIndex(fromCurrency, toCurrency)
    }
}
