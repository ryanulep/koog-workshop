package org.example.project.screens.chat

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.project.domain.character.Character
import org.example.project.domain.chat.ChatService
import org.example.project.domain.chat.ChatUpdate
import org.example.project.koog.ChatAgentProvider
import org.example.project.koog.tracking.AgentExecutionTraceEvent
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

@Serializable
private data class ToolMessage(val message: String)

class ChatViewModel(
    private val character: Character,
    initialConversationId: String,
    initialMessages: List<Message>?,
    private val chatAgentProvider: ChatAgentProvider,
    private val chatService: ChatService,
    private val historyProvider: ChatHistoryProvider,
    private val onNavigateBack: () -> Unit,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(
            title = character.name,
            chatMessages = buildList {
                add(ChatMessage.SystemMessage("Hi ${character.name}! I'm the Fantasy Store assistant. How can I help?"))
                initialMessages?.mapNotNull(::toChatMessage)?.let { addAll(it) }
            }
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sessionId: String = initialConversationId
    private var agent: AIAgent<String, String>? = null

    fun onEvent(event: ChatUiEvents) {
        viewModelScope.launch {
            when (event) {
                is ChatUiEvents.UpdateInputText -> updateInputText(event.text)
                is ChatUiEvents.ToggleDebugEnabled -> toggleDebugEnabled()
                is ChatUiEvents.ToggleDebugOption -> toggleDebugOption(event.option)
                ChatUiEvents.SendMessage -> sendMessage()
                ChatUiEvents.RestartChat -> restartChat()
                ChatUiEvents.NavigateBack -> onNavigateBack()
            }
        }
    }

    private fun toChatMessage(message: Message): ChatMessage? = when (message) {
        is Message.User -> ChatMessage.UserMessage(message.content)
        is Message.Assistant -> ChatMessage.AgentMessage(message.content)
        is Message.Reasoning -> null
        is Message.System -> null

        is Message.Tool.Call if message.tool == "askQuestion" -> {
            val text = Json.decodeFromString<ToolMessage>(message.parts.single().text).message
            ChatMessage.AgentMessage(text)
        }

        is Message.Tool.Result if message.tool == "askQuestion" ->
            ChatMessage.UserMessage(Json.decodeFromString(String.serializer(), message.content))

        is Message.Tool.Call -> ChatMessage.ToolCallMessage(
            toolName = message.tool,
            args = mapOf("args" to message.content)
        )

        is Message.Tool.Result -> null
    }

    private fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    private fun toggleDebugEnabled() {
        _uiState.update {
            it.copy(debugView = it.debugView.copy(enabled = !it.debugView.enabled))
        }
    }

    private fun toggleDebugOption(option: DebugOption) {
        _uiState.update {
            val current = it.debugView
            val newOptions = if (option in current.options) current.options - option else current.options + option
            it.copy(debugView = current.copy(options = newOptions))
        }
    }

    private fun sendMessage() {
        val userInput = _uiState.value.inputText.trim()
        if (userInput.isEmpty()) return


        if (_uiState.value.userResponseRequested) {
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true,
                    userResponseRequested = false,
                    currentUserResponse = userInput,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true,
                )
            }

            viewModelScope.launch {
                runAgent(userInput)
            }
        }
    }

    private suspend fun runAgent(userInput: String) {
        withContext(Dispatchers.Default) {
            try {
                val currentAgent = agent ?: createAgent().also { agent = it }
                val result = currentAgent.run(userInput, sessionId)
                // Write that a new chat was created
                chatService.updateChat(ChatUpdate(character.id, sessionId))

                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.AgentMessage(result),
                        isInputEnabled = true,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = true,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private suspend fun createAgent(): AIAgent<String, String> {
        val onToolCallEvent: suspend (String, Map<String, String>) -> Unit = { toolName, args ->
            _uiState.update {
                it.copy(chatMessages = it.chatMessages + ChatMessage.ToolCallMessage(toolName, args))
            }
        }
        val onErrorEvent: suspend (String) -> Unit = { errorMessage ->
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.ErrorMessage(errorMessage),
                    isInputEnabled = true,
                    isLoading = false,
                )
            }
        }
        val onLLMCallEvent: suspend (List<Message>, List<ToolDescriptor>) -> Unit =
            { messages, tools ->
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.LLMCallMessage(
                            LlmCallData(
                                messageHistory = messages.toHistoryItems(),
                                availableTools = tools.toToolData()
                            )
                        ),
                    )
                }
            }
        val onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit = { event ->
            val item = when (event) {
                is AgentExecutionTraceEvent.Node -> ExecutionTraceItem.Node(event.name)
                is AgentExecutionTraceEvent.Subgraph -> ExecutionTraceItem.Subgraph(event.name)
            }
            _uiState.update {
                it.copy(chatMessages = it.chatMessages + ChatMessage.ExecutionTraceMessage(item))
            }
        }
        val onAssistantMessage: suspend (String) -> String = { message ->
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.AgentMessage(message),
                    isInputEnabled = true,
                    isLoading = false,
                    userResponseRequested = true,
                )
            }

            val userResponse = _uiState
                .first { it.currentUserResponse != null }
                .currentUserResponse
                ?: error("User response is null")

            _uiState.update { it.copy(currentUserResponse = null) }

            userResponse
        }

        return chatAgentProvider.provideAgent(
            characterId = character.id,
            historyProvider = historyProvider,
            onToolCallEvent = onToolCallEvent,
            onLLMCallEvent = onLLMCallEvent,
            onErrorEvent = onErrorEvent,
            onExecutionTraceEvent = onExecutionTraceEvent,
            onAssistantMessage = onAssistantMessage,
        )
    }

    private fun restartChat() {
        sessionId = Uuid.random().toString()
        agent = null
        _uiState.update {
            ChatUiState(
                title = character.name,
                chatMessages = listOf(
                    ChatMessage.SystemMessage("Hi ${character.name}! I'm the Fantasy Store assistant. How can I help?")
                )
            )
        }
    }

    companion object {
        fun factory(
            character: Character,
            conversationId: String,
            initialMessages: List<Message>?,
            chatAgentProvider: ChatAgentProvider,
            chatService: ChatService,
            historyProvider: ChatHistoryProvider,
            onNavigateBack: () -> Unit,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return ChatViewModel(
                        character = character,
                        initialConversationId = conversationId,
                        initialMessages = initialMessages,
                        chatAgentProvider = chatAgentProvider,
                        chatService = chatService,
                        historyProvider = historyProvider,
                        onNavigateBack = onNavigateBack,
                    ) as T
                }
            }
    }
}
