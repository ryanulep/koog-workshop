package org.example.project.domain.currency

import org.example.project.domain.currency.CurrencyRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.shared.CurrencyConversionId
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.currency.Currency
import org.example.project.domain.currency.CurrencyConversion
import org.jetbrains.exposed.v1.jdbc.Database

class CurrencyService(
    private val database: Database,
    private val currencyRepository: CurrencyRepository = CurrencyRepository()
) {
    suspend fun getAllCurrencies(): List<Currency> =
        database.suspendTransaction { currencyRepository.getAllCurrencies() }

    suspend fun getCurrencyOrNull(id: CurrencyId): Currency? =
        database.suspendTransaction { currencyRepository.getCurrencyOrNull(id) }

    suspend fun createCurrency(
        code: String,
        name: String,
        symbol: String,
        iconPath: String? = null
    ): CurrencyId =
        database.suspendTransaction {
            currencyRepository.createCurrency(code, name, symbol, iconPath)
        }

    suspend fun updateCurrency(
        id: CurrencyId,
        code: String? = null,
        name: String? = null,
        symbol: String? = null,
        iconPath: String? = null
    ): Boolean =
        database.suspendTransaction {
            currencyRepository.updateCurrency(id, code, name, symbol, iconPath)
        }

    suspend fun deleteCurrency(id: CurrencyId): Boolean =
        database.suspendTransaction { currencyRepository.deleteCurrency(id) }

    suspend fun getConversionRateOrNull(fromId: CurrencyId, toId: CurrencyId): Double? =
        database.suspendTransaction { currencyRepository.getConversionRateOrNull(fromId, toId) }

    suspend fun getAllConversionRates(): List<CurrencyConversion> =
        database.suspendTransaction { currencyRepository.getAllConversionRates() }

    suspend fun setConversionRate(
        fromId: CurrencyId,
        toId: CurrencyId,
        rate: Double
    ): CurrencyConversionId {
        require(rate > 0) { "Conversion rate must be positive" }
        return database.suspendTransaction {
            val existing = currencyRepository.getConversionRateOrNull(fromId, toId)
            if (existing != null) {
                val conversion = currencyRepository.getAllConversionRates()
                    .first { it.fromCurrencyId == fromId && it.toCurrencyId == toId }
                currencyRepository.updateConversionRate(conversion.id, rate)
                conversion.id
            } else {
                currencyRepository.createConversionRate(fromId, toId, rate)
            }
        }
    }

    suspend fun deleteConversionRate(id: CurrencyConversionId): Boolean =
        database.suspendTransaction { currencyRepository.deleteConversionRate(id) }
}
