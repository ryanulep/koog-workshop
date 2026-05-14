package org.example.project.domain.character

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.example.project.domain.shared.*

@Serializable
data class Character(
    val id: CharacterId,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class TransactionType {
    DEPOSIT,
    PURCHASE,
    REFUND,
    EXCHANGE_DEBIT,
    EXCHANGE_CREDIT
}

@Serializable
data class WalletBalance(
    val currencyId: CurrencyId,
    val currencyCode: String,
    val currencyName: String,
    val balance: Long
)

@Serializable
data class Transaction(
    val id: TransactionId,
    val characterId: CharacterId,
    val currencyId: CurrencyId,
    val amount: Long,
    val type: TransactionType,
    val referenceId: Uuid?,
    val referenceType: String?,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class CreateCharacterRequest(val name: String)

@Serializable
data class UpdateCharacterRequest(val name: String)

@Serializable
data class DepositRequest(
    val currencyId: CurrencyId,
    val amount: Long,
    val description: String? = null
)
