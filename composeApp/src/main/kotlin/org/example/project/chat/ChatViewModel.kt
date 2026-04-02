package org.example.project.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.chat.ChatUi.Message.User
import org.example.project.chat.agent.ChatAgent
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

data class ChatUi(
    val messages: PersistentList<Message> = persistentListOf(),
    val inputText: String = "",
    val isSending: Boolean = false
) {
    sealed interface Message {
        val id: String
        val content: String

        data class User(val user: String) : Message {
            override val id: String = Uuid.random().toString()
            override val content: String = user
        }
        data class CustomerSupport(val customerSupport: String) : Message {
            override val id: String = Uuid.random().toString()
            override val content: String = customerSupport
        }
    }
}

class ChatViewModel(
    private val session: String,
    private val chat: ChatAgent
) : ViewModel() {

    val uiState: StateFlow<ChatUi>
        field = MutableStateFlow(ChatUi())

    fun loadHistory() = viewModelScope.launch {
        val messages = chat.loadChat(session)
        uiState.value = uiState.value.copy(messages = messages)
    }

    fun updateInputText(text: String) {
        uiState.value = uiState.value.copy(inputText = text)
    }

    fun sendMessage() = viewModelScope.launch {
        val message = uiState.value.inputText.trim()
        if (message.isEmpty()) return@launch

        uiState.value = uiState.value.copy(
            inputText = "",
            isSending = true,
            messages = uiState.value.messages.add(User(message))
        )

        val reply = chat.sendMessage(message)

        uiState.value = uiState.value.copy(
            isSending = false,
            messages = uiState.value.messages.add(reply)
        )
    }

    companion object {
        fun factory(session: String, chat: ChatAgent): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return ChatViewModel(session, chat) as T
                }
            }
    }
}
