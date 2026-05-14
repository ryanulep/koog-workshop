package org.example.project.screens.chatlist

import ai.koog.prompt.message.Message
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.character.Character
import org.example.project.domain.chat.ChatDetails
import org.example.project.screens.chat.ChatService
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

data class ChatListUiState(
    val character: Character,
    val chats: List<ChatDetails> = emptyList(),
    val isLoading: Boolean = true,
)

class ChatListViewModel(
    private val character: Character,
    private val chatService: ChatService,
    private val onNavigateBack: () -> Unit,
    private val onChatSelected: (conversationId: String, messages: List<Message>) -> Unit,
    private val onNewChat: (conversationId: String) -> Unit,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatListUiState(character = character))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    fun loadChats() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val chats = chatService.getCharacterChatDetails(character.id)
        _uiState.update { it.copy(chats = chats, isLoading = false) }
    }

    fun selectChat(chatDetails: ChatDetails) {
        onChatSelected(chatDetails.conversationId, chatDetails.messages)
    }

    fun startNewChat() {
        onNewChat(Uuid.random().toString())
    }

    fun navigateBack() {
        onNavigateBack()
    }

    companion object {
        fun factory(
            character: Character,
            chatService: ChatService,
            onNavigateBack: () -> Unit,
            onChatSelected: (conversationId: String, messages: List<Message>) -> Unit,
            onNewChat: (conversationId: String) -> Unit,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return ChatListViewModel(
                    character = character,
                    chatService = chatService,
                    onNavigateBack = onNavigateBack,
                    onChatSelected = onChatSelected,
                    onNewChat = onNewChat,
                ) as T
            }
        }
    }
}
