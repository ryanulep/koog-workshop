package org.example.project.domain.character

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.example.project.domain.shared.*

@Immutable
data class WalletBalance(
    val currencyId: CurrencyId,
    val currencyCode: String,
    val currencyName: String,
    val balance: Long
)

@Immutable
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
