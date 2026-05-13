package org.example.project.domain.currency

import org.example.project.domain.currency.CurrencyRepository

import org.example.project.domain.shared.CurrencyConversionId
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.currency.Currency
import org.example.project.domain.currency.CurrencyConversion
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class CurrencyService(
    private val currencyRepository: CurrencyRepository
) {
    fun getAllCurrencies(): List<Currency> =
        currencyRepository.getAllCurrencies()

    fun getCurrencyOrNull(id: CurrencyId): Currency? =
        currencyRepository.getCurrencyOrNull(id)

    fun createCurrency(
        code: String,
        name: String,
        symbol: String,
        iconPath: String? = null
    ): CurrencyId =
        currencyRepository.createCurrency(code, name, symbol, iconPath)

    fun updateCurrency(
        id: CurrencyId,
        code: String? = null,
        name: String? = null,
        symbol: String? = null,
        iconPath: String? = null
    ): Boolean =
        currencyRepository.updateCurrency(id, code, name, symbol, iconPath)

    fun deleteCurrency(id: CurrencyId): Boolean =
        currencyRepository.deleteCurrency(id)

    fun getConversionRateOrNull(fromId: CurrencyId, toId: CurrencyId): Double? =
        currencyRepository.getConversionRateOrNull(fromId, toId)

    fun getAllConversionRates(): List<CurrencyConversion> =
        currencyRepository.getAllConversionRates()

    fun setConversionRate(
        fromId: CurrencyId,
        toId: CurrencyId,
        rate: Double
    ): CurrencyConversionId {
        require(rate > 0) { "Conversion rate must be positive" }
        val existing = currencyRepository.getConversionRateOrNull(fromId, toId)
        return if (existing != null) {
            val conversion = currencyRepository.getAllConversionRates()
                .first { it.fromCurrencyId == fromId && it.toCurrencyId == toId }
            currencyRepository.updateConversionRate(conversion.id, rate)
            conversion.id
        } else {
            currencyRepository.createConversionRate(fromId, toId, rate)
        }
    }

    fun deleteConversionRate(id: CurrencyConversionId): Boolean =
        currencyRepository.deleteConversionRate(id)
}
