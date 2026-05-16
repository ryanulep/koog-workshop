package com.jetbrains.koog.workshop.screens.agentdemo

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.koog.workshop.agents.util.AgentProvider
import com.jetbrains.koog.workshop.agents.util.AgentExecutionTraceEvent
import com.jetbrains.koog.workshop.agents.util.ChatAgentProvider
import com.jetbrains.koog.workshop.agents.util.TaskAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            agentAvatarRes = agentProvider.avatarRes,
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
                is AgentDemoUiEvents.ToggleDebugEnabled -> toggleDebugEnabled()
                is AgentDemoUiEvents.ToggleDebugOption -> toggleDebugOption(event.option)
                AgentDemoUiEvents.SendMessage -> sendMessage()
                AgentDemoUiEvents.RestartChat -> restartChat()
                AgentDemoUiEvents.NavigateBack -> navigationCallback.goBack()
            }
        }
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

        // If the agent is waiting for a response to a question
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
            // Initial message flow - add user message and start the agent
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

                _uiState.update {
                    when (agentProvider) {
                        is TaskAgentProvider -> it.copy(
                            chatMessages = it.chatMessages +
                                    ChatMessage.ResultMessage(result) +
                                    ChatMessage.SystemMessage("The agent has stopped."),
                            isInputEnabled = false,
                            isLoading = false,
                            isChatEnded = true,
                        )

                        is ChatAgentProvider -> it.copy(
                            chatMessages = it.chatMessages + ChatMessage.AgentMessage(result),
                            isInputEnabled = true,
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = !_uiState.value.isChatEnded,
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
                is AgentExecutionTraceEvent.SubgraphStarted -> ExecutionTraceItem.SubgraphStarted(event.name)
                is AgentExecutionTraceEvent.SubgraphCompleted -> ExecutionTraceItem.SubgraphCompleted(event.name, event.result)
            }
            _uiState.update {
                it.copy(chatMessages = it.chatMessages + ChatMessage.ExecutionTraceMessage(item))
            }
        }

        return when (val provider = agentProvider) {
            is ChatAgentProvider -> provider.provideAgent(
                historyProvider = chatHistoryProvider,
                onToolCallEvent = onToolCallEvent,
                onLLMCallEvent = onLLMCallEvent,
                onErrorEvent = onErrorEvent,
                onExecutionTraceEvent = onExecutionTraceEvent,
            )

            is TaskAgentProvider -> provider.provideAgent(
                historyProvider = chatHistoryProvider,
                onToolCallEvent = onToolCallEvent,
                onLLMCallEvent = onLLMCallEvent,
                onErrorEvent = onErrorEvent,
                onExecutionTraceEvent = onExecutionTraceEvent,
                onAssistantMessage = { message ->
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
                        ?: throw IllegalStateException("User response is null")

                    _uiState.update {
                        it.copy(currentUserResponse = null)
                    }

                    userResponse
                },
            )
        }
    }

    private fun restartChat() {
        chatHistoryProvider = InMemoryChatHistoryProvider()
        sessionId = UUID.randomUUID().toString()
        agent = null
        _uiState.update {
            AgentDemoUiState(
                title = agentProvider.title,
                agentAvatarRes = agentProvider.avatarRes,
                chatMessages = listOf(ChatMessage.SystemMessage(agentProvider.description))
            )
        }
    }
}
