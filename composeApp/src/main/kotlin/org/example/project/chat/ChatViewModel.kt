package org.example.project.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.chat.ChatMemory
import org.example.project.domain.chat.ChatMessage
import kotlin.reflect.KClass

data class ChatUiState(
    val messages: PersistentList<ChatMessage> = persistentListOf(),
    val inputText: String = "",
    val isSending: Boolean = false
)

class ChatViewModel(
    private val chatMemory: ChatMemory
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() = viewModelScope.launch {
        val message = _uiState.value.inputText.trim()
        if (message.isEmpty()) return@launch

        _uiState.value = _uiState.value.copy(
            inputText = "",
            isSending = true
        )

        chatMemory.sendMessage(message)
        refresh()

        _uiState.value = _uiState.value.copy(isSending = false)
    }

    private fun refresh() {
        _uiState.value = _uiState.value.copy(
            messages = chatMemory.mes
        )
    }

    companion object {
        fun factory(chatMemory: ChatMemory): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return ChatViewModel(chatMemory) as T
                }
            }
    }
}
