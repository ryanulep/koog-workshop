package org.example.project.domain.model

import org.example.project.domain.enums.TransactionType

data class Currency(
    val id: Long,
    val code: String,
    val name: String,
    val symbol: String,
    val iconPath: String?
)

data class CurrencyConversion(
    val id: Long,
    val fromCurrencyId: Long,
    val toCurrencyId: Long,
    val rate: Double
)

data class WalletBalance(
    val currencyId: Long,
    val currencyCode: String,
    val currencyName: String,
    val balance: Long
)

data class Transaction(
    val id: Long,
    val characterId: Long,
    val currencyId: Long,
    val amount: Long,
    val type: TransactionType,
    val referenceId: Long?,
    val referenceType: String?,
    val description: String?,
    val createdAt: Long
)
