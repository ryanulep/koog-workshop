package org.example.project.db.repository

import org.example.project.db.suspendTransaction
import org.example.project.db.tables.Characters
import org.example.project.db.tables.Transactions
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.id.TransactionId
import org.example.project.domain.model.Character
import org.example.project.domain.repository.CharacterRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ExposedCharacterRepository(
    private val database: Database
) : CharacterRepository {

    override suspend fun getCharacterOrNull(id: CharacterId): Character? = database.suspendTransaction {
        Characters.select(Characters.id, Characters.name, Characters.createdAt)
            .where { Characters.id eq id.value }
            .map(::mapToCharacter)
            .singleOrNull()
    }

    override suspend fun createCharacter(name: String): CharacterId = database.suspendTransaction {
        CharacterId(
            Characters.insertAndGetId {
                it[Characters.name] = name
            }.value
        )
    }

    override suspend fun getWalletBalance(characterId: CharacterId): Map<CurrencyId, Long> = database.suspendTransaction {
        Transactions.select(Transactions.currency, Transactions.amount)
            .where { Transactions.character eq characterId.value }
            .groupBy { it[Transactions.currency].value }
            .map { (currencyId, rows) ->
                CurrencyId(currencyId) to rows.sumOf { it[Transactions.amount] }
            }.toMap()
    }

    override suspend fun addTransaction(
        characterId: CharacterId,
        currencyId: CurrencyId,
        amount: Long,
        type: TransactionType,
        referenceId: Uuid?,
        referenceType: String?,
        description: String?
    ): TransactionId = database.suspendTransaction {
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
    }

    private fun mapToCharacter(row: ResultRow) = Character(
        id = CharacterId(row[Characters.id].value),
        name = row[Characters.name],
        createdAt = row[Characters.createdAt],
        updatedAt = row[Characters.updatedAt]
    )
}
