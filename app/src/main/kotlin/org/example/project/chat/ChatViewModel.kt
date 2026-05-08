package org.example.project.chat

import ai.koog.prompt.message.Message
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.project.domain.chat.ChatService
import org.example.project.domain.shared.CharacterId
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

@Serializable
private data class ToolMessage(val message: String)

class ChatViewModel(
    private val session: Uuid,
    private val chatAgent: ChatAgent,
    private val chatService: ChatService,
) : ViewModel() {
    val uiState: StateFlow<ChatUi>
        field = MutableStateFlow(ChatUi())

    private var state: ChatState = ChatState.Idle

    private fun updateState(state: ChatState, update: ChatUi.() -> ChatUi) {
        this.state = state
        uiState.value = uiState.value.update().copy(isWaiting = state is ChatState.WaitingForAgent)
    }

    fun loadHistory() = viewModelScope.launch {
        val messages = chatService
            .getChatHistory(session.toString())
            .mapNotNull { message ->
                // Convert Koog messages to appropriate UI representation
                when (message) {
                    is Message.User -> ChatUi.Message.User(message.content)
                    is Message.Assistant -> ChatUi.Message.CustomerSupport(message.content)
                    is Message.Reasoning -> ChatUi.Message.CustomerSupport(message.content)
                    is Message.System -> ChatUi.Message.CustomerSupport(message.content)

                    is Message.Tool.Call if message.tool == "askQuestion" -> {
                        val message = Json.decodeFromString<ToolMessage>(message.parts.single().text).message
                        ChatUi.Message.CustomerSupport(message)
                    }

                    is Message.Tool.Result if message.tool == "askQuestion" ->
                        ChatUi.Message.User(Json.decodeFromString(String.serializer(), message.content))

                    is Message.Tool.Result,
                    is Message.Tool.Call -> null
                }
            }
            .toPersistentList()

        uiState.value = uiState.value.copy(messages = messages)
    }

    fun updateInputText(text: String) {
        uiState.value = uiState.value.copy(inputText = text)
    }

    fun sendMessage(characterId: CharacterId) = viewModelScope.launch {
        val message = uiState.value.inputText.trim()
        if (message.isEmpty()) return@launch

        when (val current = state) {
            is ChatState.AwaitingUserAnswer -> {
                updateState(ChatState.WaitingForAgent) {
                    copy(
                        inputText = "",
                        messages = messages.add(ChatUi.Message.User(message))
                    )
                }
                current.deferred.complete(message)
            }

            is ChatState.Idle -> {
                updateState(ChatState.WaitingForAgent) {
                    copy(
                        inputText = "",
                        messages = messages.add(ChatUi.Message.User(message))
                    )
                }

                val reply = chatAgent.sendMessage(characterId, session, message) { question ->
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
        fun factory(session: Uuid, chatAgent: ChatAgent, chatService: ChatService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return ChatViewModel(session, chatAgent, chatService) as T
                }
            }
    }
}
