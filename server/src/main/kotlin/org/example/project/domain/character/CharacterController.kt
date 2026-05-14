package org.example.project.domain.character

import kotlinx.serialization.Serializable
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.Page
import org.example.project.domain.shared.TransactionId
import org.springframework.web.bind.annotation.*
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/characters")
class CharacterController(
    private val characterService: CharacterService
) {

    @GetMapping
    suspend fun listCharacters(): List<Character> {
        return characterService.listCharacters()
    }

    @GetMapping("/{id}")
    suspend fun getCharacter(@PathVariable id: String): Character? {
        return characterService.getCharacterOrNull(CharacterId(Uuid.parse(id)))
    }

    @PostMapping
    suspend fun createCharacter(@RequestBody request: CreateCharacterRequest): CharacterId {
        return characterService.createCharacter(request.name)
    }

    @PutMapping("/{id}")
    suspend fun updateCharacter(
        @PathVariable id: String,
        @RequestBody request: UpdateCharacterRequest
    ): Boolean {
        return characterService.updateCharacter(CharacterId(Uuid.parse(id)), request.name)
    }

    @DeleteMapping("/{id}")
    suspend fun deleteCharacter(@PathVariable id: String): Boolean {
        return characterService.deleteCharacter(CharacterId(Uuid.parse(id)))
    }

    @GetMapping("/{id}/balance")
    suspend fun getWalletBalance(@PathVariable id: String): Map<CurrencyId, Long> {
        return characterService.getWalletBalance(CharacterId(Uuid.parse(id)))
    }

    @PostMapping("/{id}/deposit")
    suspend fun deposit(
        @PathVariable id: String,
        @RequestBody request: DepositRequest
    ): TransactionId {
        return characterService.deposit(
            characterId = CharacterId(Uuid.parse(id)),
            currencyId = request.currencyId,
            amount = request.amount,
            description = request.description
        )
    }

    @GetMapping("/{id}/history")
    suspend fun getTransactionHistory(
        @PathVariable id: String,
        @RequestParam(defaultValue = "0") offset: Long,
        @RequestParam(defaultValue = "50") limit: Long
    ): Page<Transaction> {
        return characterService.getTransactionHistory(CharacterId(Uuid.parse(id)), offset, limit)
    }
}
