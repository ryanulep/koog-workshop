package org.example.project.domain.character

import org.example.project.domain.character.CharacterRepository

import org.example.project.domain.character.TransactionType
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.TransactionId
import org.example.project.domain.character.Character
import org.example.project.domain.shared.Page
import org.example.project.domain.character.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class CharacterService(
    private val characterRepository: CharacterRepository
) {
    fun listCharacters(): List<Character> =
        characterRepository.listCharacters()

    fun getCharacterOrNull(id: CharacterId): Character? =
        characterRepository.getCharacterOrNull(id)

    fun createCharacter(name: String): CharacterId =
        characterRepository.createCharacter(name)

    fun updateCharacter(id: CharacterId, name: String): Boolean =
        characterRepository.updateCharacter(id, name)

    fun deleteCharacter(id: CharacterId): Boolean =
        characterRepository.deleteCharacter(id)

    fun getWalletBalance(characterId: CharacterId): Map<CurrencyId, Long> =
        characterRepository.getWalletBalance(characterId)

    fun deposit(
        characterId: CharacterId,
        currencyId: CurrencyId,
        amount: Long,
        description: String? = null
    ): TransactionId {
        require(amount > 0) { "Deposit amount must be positive" }
        return characterRepository.addTransaction(
            characterId = characterId,
            currencyId = currencyId,
            amount = amount,
            type = TransactionType.DEPOSIT,
            description = description
        )
    }

    fun getTransactionHistory(
        characterId: CharacterId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Transaction> =
        characterRepository.getTransactionHistory(characterId, offset, limit)
}
