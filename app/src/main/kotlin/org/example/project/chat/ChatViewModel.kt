package org.example.project.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.chat.ChatUi.Message.User
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

sealed interface ChatState {
    /** User can type and send a new message to the agent. */
    data object Idle : ChatState

    /** Agent is processing — input disabled. */
    data object WaitingForAgent : ChatState

    /** Agent asked a question — user input completes the deferred. */
    class AwaitingUserAnswer(val deferred: CompletableDeferred<String>) : ChatState
}

data class ChatUi(
    val messages: PersistentList<Message> = persistentListOf(),
    val inputText: String = "",
    val isWaiting: Boolean = false
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

    private var state: ChatState = ChatState.Idle

    private fun updateState(state: ChatState, update: ChatUi.() -> ChatUi) {
        this.state = state
        uiState.value = uiState.value.update().copy(isWaiting = state is ChatState.WaitingForAgent)
    }

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

        when (val current = state) {
            is ChatState.AwaitingUserAnswer -> {
                updateState(ChatState.WaitingForAgent) {
                    copy(
                        inputText = "",
                        messages = messages.add(User(message))
                    )
                }
                current.deferred.complete(message)
            }

            is ChatState.Idle -> {
                updateState(ChatState.WaitingForAgent) {
                    copy(
                        inputText = "",
                        messages = messages.add(User(message))
                    )
                }

                val reply = chat.sendMessage(message) { question ->
                    val deferred = CompletableDeferred<String>()
                    updateState(ChatState.AwaitingUserAnswer(deferred)) {
                        copy(messages = messages.add(ChatUi.Message.CustomerSupport(question)))
                    }
                    deferred.await()
                }

                updateState(ChatState.Idle) {
                    copy(messages = messages.add(reply))
                }
            }

            is ChatState.WaitingForAgent -> Unit
        }
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
