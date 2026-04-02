package org.example.project.domain.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import java.util.UUID

interface ChatAgent {
    suspend fun sendMessage(userMessage: String): String
}

class ChatMemory(private val chatAgent: ChatAgent) {
    val messages: StateFlow<List<ChatMessage>>
        field = MutableStateFlow(emptyList())

    suspend fun sendMessage(userMessage: String) {
        addMessage(userMessage, MessageRole.User)

        val response = chatAgent.sendMessage(userMessage)
        addMessage(response, MessageRole.Assistant)
    }

    private fun addMessage(content: String, role: MessageRole) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            role = role,
            timestamp = Clock.System.now()
        )
        messages.update { it + message }
    }
}
