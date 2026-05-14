package org.example.project.screens.chat

import ai.koog.prompt.message.Message
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.domain.chat.ChatDetails
import org.example.project.domain.chat.ChatUpdate
import org.example.project.domain.shared.CharacterId

class ChatService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    /**
     * Fetch all chats for the current user with the chat history
     */
    suspend fun getCharacterChatDetails(characterId: CharacterId): List<ChatDetails> {
        return httpClient.get("$baseUrl/chats/character/${characterId.value}").body()
    }

    /**
     * Create new character <-> conversation relationship or notify that it was updated (e.g. new messages)
     */
    suspend fun updateChat(update: ChatUpdate) {
        httpClient.post("$baseUrl/chats/update") {
            contentType(ContentType.Application.Json)
            setBody(update)
        }
    }

    suspend fun getChatHistory(conversationId: String): List<Message> {
        return httpClient.get("$baseUrl/chats/history/$conversationId").body()
    }

    suspend fun answerQuestion(characterId: CharacterId, sessionId: String, answer: String) {
        httpClient.post("$baseUrl/chats/answer") {
            parameter("characterId", characterId.value.toString())
            parameter("sessionId", sessionId)
            parameter("answer", answer)
        }
    }
}