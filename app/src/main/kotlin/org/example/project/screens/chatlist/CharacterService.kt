package org.example.project.screens.chatlist

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.domain.character.Character
import org.example.project.domain.character.CreateCharacterRequest
import org.example.project.domain.character.DepositRequest
import org.example.project.domain.character.Transaction
import org.example.project.domain.character.UpdateCharacterRequest
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.Page
import org.example.project.domain.shared.TransactionId

class CharacterService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    suspend fun listCharacters(): List<Character> =
        httpClient.get("$baseUrl/characters").body()

    suspend fun getCharacterOrNull(id: CharacterId): Character? =
        httpClient.get("$baseUrl/characters/${id.value}").body()

    suspend fun createCharacter(name: String): CharacterId =
        httpClient.post("$baseUrl/characters") {
            contentType(ContentType.Application.Json)
            setBody(CreateCharacterRequest(name))
        }.body()

    suspend fun updateCharacter(id: CharacterId, name: String): Boolean =
        httpClient.put("$baseUrl/characters/${id.value}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateCharacterRequest(name))
        }.body()

    suspend fun deleteCharacter(id: CharacterId): Boolean =
        httpClient.delete("$baseUrl/characters/${id.value}").body()

    suspend fun getWalletBalance(characterId: CharacterId): Map<CurrencyId, Long> =
        httpClient.get("$baseUrl/characters/${characterId.value}/balance").body()

    suspend fun deposit(
        characterId: CharacterId,
        currencyId: CurrencyId,
        amount: Long,
        description: String? = null
    ): TransactionId {
        require(amount > 0) { "Deposit amount must be positive" }
        return httpClient.post("$baseUrl/characters/${characterId.value}/deposit") {
            contentType(ContentType.Application.Json)
            setBody(DepositRequest(currencyId, amount, description))
        }.body()
    }

    suspend fun getTransactionHistory(
        characterId: CharacterId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Transaction> =
        httpClient.get("$baseUrl/characters/${characterId.value}/history") {
            parameter("offset", offset)
            parameter("limit", limit)
        }.body()
}