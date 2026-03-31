package org.example.project.db.repository

import org.example.project.db.tables.Characters
import org.example.project.db.tables.Transactions
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.id.TransactionId
import org.example.project.domain.model.Character
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ExposedCharacterRepository {

    context(_: Transaction)
    fun getCharacterOrNull(id: CharacterId): Character? =
        Characters.selectAll()
            .where { Characters.id eq id.value }
            .map(::mapToCharacter)
            .singleOrNull()

    context(_: Transaction)
    fun createCharacter(name: String): CharacterId =
        CharacterId(
            Characters.insertAndGetId {
                it[Characters.name] = name
            }.value
        )

    context(_: Transaction)
    fun getWalletBalance(characterId: CharacterId): Map<CurrencyId, Long> =
        Transactions.select(Transactions.currency, Transactions.amount)
            .where { Transactions.character eq characterId.value }
            .groupBy { it[Transactions.currency].value }
            .map { entry ->
                CurrencyId(entry.key) to entry.value.sumOf { row -> row[Transactions.amount] }
            }.toMap()

    context(_: Transaction)
    fun addTransaction(
        characterId: CharacterId,
        currencyId: CurrencyId,
        amount: Long,
        type: TransactionType,
        referenceId: Uuid? = null,
        referenceType: String? = null,
        description: String? = null
    ): TransactionId =
        TransactionId(
            Transactions.insertAndGetId {
                it[Transactions.character] = characterId.value
                it[Transactions.currency] = currencyId.value
                it[Transactions.amount] = amount
                it[Transactions.type] = type.name
                it[Transactions.referenceId] = referenceId
                it[Transactions.referenceType] = referenceType
                it[Transactions.description] = description
            }.value
        )

    private fun mapToCharacter(row: ResultRow) = Character(
        id = CharacterId(row[Characters.id].value),
        name = row[Characters.name],
        createdAt = row[Characters.createdAt],
        updatedAt = row[Characters.updatedAt]
    )
}
