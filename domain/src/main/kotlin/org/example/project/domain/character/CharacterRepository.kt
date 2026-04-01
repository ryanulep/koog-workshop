package org.example.project.domain.character

import org.example.project.domain.character.Characters
import org.example.project.domain.character.Transactions
import org.example.project.domain.character.TransactionType
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.TransactionId
import org.example.project.domain.character.Character
import org.example.project.domain.shared.Page
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.example.project.db.update as storeUpdate
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class CharacterRepository {

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
    fun updateCharacter(id: CharacterId, name: String): Boolean =
        Characters.storeUpdate({ Characters.id eq id.value }) {
            it[Characters.name] = name
        } > 0

    context(_: Transaction)
    fun deleteCharacter(id: CharacterId): Boolean =
        Characters.deleteWhere { Characters.id eq id.value } > 0

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

    context(_: Transaction)
    fun getTransactionHistory(characterId: CharacterId, offset: Long, limit: Long): Page<org.example.project.domain.character.Transaction> {
        val query = Transactions.selectAll().where { Transactions.character eq characterId.value }
            .orderBy(Transactions.createdAt, SortOrder.DESC)
        val items = query.copy()
            .limit(limit.toInt()).offset(offset)
            .map(::mapToTransaction)
        val total = query.count()
        return Page(items, total, offset, limit)
    }

    private fun mapToCharacter(row: ResultRow) = Character(
        id = CharacterId(row[Characters.id].value),
        name = row[Characters.name],
        createdAt = row[Characters.createdAt],
        updatedAt = row[Characters.updatedAt]
    )

    private fun mapToTransaction(row: ResultRow) = Transaction(
        id = TransactionId(row[Transactions.id].value),
        characterId = CharacterId(row[Transactions.character].value),
        currencyId = CurrencyId(row[Transactions.currency].value),
        amount = row[Transactions.amount],
        type = TransactionType.valueOf(row[Transactions.type]),
        referenceId = row[Transactions.referenceId],
        referenceType = row[Transactions.referenceType],
        description = row[Transactions.description],
        createdAt = row[Transactions.createdAt],
        updatedAt = row[Transactions.updatedAt]
    )
}
