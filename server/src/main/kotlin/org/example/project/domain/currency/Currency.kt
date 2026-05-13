package org.example.project.domain.currency

import kotlin.time.Instant
import org.example.project.domain.shared.*

data class Currency(
    val id: CurrencyId,
    val code: String,
    val name: String,
    val symbol: String,
    val iconPath: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CurrencyConversion(
    val id: CurrencyConversionId,
    val fromCurrencyId: CurrencyId,
    val toCurrencyId: CurrencyId,
    val rate: Double,
    val createdAt: Instant,
    val updatedAt: Instant
)
