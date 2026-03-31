package org.example.project.db.repository

import org.example.project.db.suspendTransaction
import org.example.project.db.tables.Characters
import org.example.project.db.tables.Transactions
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.model.Character
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class CharacterRepository(
    private val database: Database
) {

    suspend fun getCharacter(id: Long): Character? = database.suspendTransaction {
        Characters.select(Characters.id, Characters.name, Characters.createdAt)
            .where { Characters.id eq id }
            .map(::mapToCharacter)
            .singleOrNull()
    }

    suspend fun createCharacter(name: String): Long = database.suspendTransaction {
        Characters.insertAndGetId {
            it[Characters.name] = name
        }.value
    }

    suspend fun getWalletBalance(characterId: Long): Map<Long, Long> = database.suspendTransaction {
        Transactions.select(Transactions.currency, Transactions.amount)
            .where {  Transactions.character eq characterId }
            .groupBy { it[Transactions.currency].value }
            .mapValues { entry -> entry.value.sumOf { it[Transactions.amount] } }
    }

    suspend fun addTransaction(
        characterId: Long,
        currencyId: Long,
        amount: Long,
        type: TransactionType,
        referenceId: Long? = null,
        referenceType: String? = null,
        description: String? = null
    ): Long = database.suspendTransaction {
        Transactions.insertAndGetId {
            it[Transactions.character] = characterId
            it[Transactions.currency] = currencyId
            it[Transactions.amount] = amount
            it[Transactions.type] = type.name
            it[Transactions.referenceId] = referenceId
            it[Transactions.referenceType] = referenceType
            it[Transactions.description] = description
        }.value
    }

    private fun mapToCharacter(row: ResultRow) = Character(
        id = row[Characters.id].value,
        name = row[Characters.name],
        createdAt = row[Characters.createdAt]
    )
}
