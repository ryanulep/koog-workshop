package com.jetbrains.example.koog.compose.screens.agentdemo

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.example.koog.compose.agents.common.AgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AgentDemoViewModel(
    private val navigationCallback: AgentDemoNavigationCallback,
    private val agentProvider: AgentProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AgentDemoUiState(
            title = agentProvider.title,
            chatMessages = listOf(ChatMessage.SystemMessage(agentProvider.description))
        )
    )
    val uiState: StateFlow<AgentDemoUiState> = _uiState.asStateFlow()

    private var chatHistoryProvider: ChatHistoryProvider = InMemoryChatHistoryProvider()
    private var sessionId: String = UUID.randomUUID().toString()
    private var agent: AIAgent<String, String>? = null

    fun onEvent(event: AgentDemoUiEvents) {
        viewModelScope.launch {
            when (event) {
                is AgentDemoUiEvents.UpdateInputText -> updateInputText(event.text)
                is AgentDemoUiEvents.UpdateDebugView -> updateDebugView(event.debugView)
                AgentDemoUiEvents.SendMessage -> sendMessage()
                AgentDemoUiEvents.RestartChat -> restartChat()
                AgentDemoUiEvents.NavigateBack -> navigationCallback.goBack()
            }
        }
    }

    private fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    private fun updateDebugView(debugView: DebugView) {
        _uiState.update { it.copy(debugView = debugView) }
    }

    private fun sendMessage() {
        val userInput = _uiState.value.inputText.trim()
        if (userInput.isEmpty()) return

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + ChatMessage.UserMessage(userInput),
                inputText = "",
                isInputEnabled = false,
                isLoading = true
            )
        }

        viewModelScope.launch {
            runAgent(userInput)
        }
    }

    private suspend fun runAgent(userInput: String) {
        withContext(Dispatchers.Default) {
            try {
                val currentAgent = agent ?: agentProvider.provideAgent(
                    historyProvider = chatHistoryProvider,
                    onToolCallEvent = { toolName, args ->
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(chatMessages = it.chatMessages + ChatMessage.ToolCallMessage(toolName, args))
                            }
                        }
                    },
                    onErrorEvent = { errorMessage ->
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(
                                    chatMessages = it.chatMessages + ChatMessage.ErrorMessage(errorMessage),
                                    isInputEnabled = true,
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onLLMCallEvent = { messages, tools ->
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(
                                    chatMessages = it.chatMessages + ChatMessage.LLMCallMessage(
                                        LlmCallData(
                                            messageHistory = messages.toHistoryItems(),
                                            availableTools = tools.toToolData()
                                        )
                                    ),
                                    isInputEnabled = true,
                                    isLoading = false
                                )
                            }
                        }
                    }
                ).also { agent = it }

                val result = currentAgent.run(userInput, sessionId)

                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.AgentMessage(result),
                        isInputEnabled = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = true,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun restartChat() {
        chatHistoryProvider = InMemoryChatHistoryProvider()
        sessionId = UUID.randomUUID().toString()
        agent = null
        _uiState.update {
            AgentDemoUiState(
                title = agentProvider.title,
                chatMessages = listOf(ChatMessage.SystemMessage(agentProvider.description))
            )
        }
    }
}
