package com.jetbrains.example.koog.compose.screens.agentdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jetbrains.example.koog.compose.theme.AppDimension
import com.jetbrains.example.koog.compose.theme.AppTheme
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

internal const val MAX_BUBBLE_WIDTH_FRACTION = 0.85f

@Composable
fun AgentDemoScreen(viewModel: AgentDemoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    AgentDemoScreenContent(
        title = uiState.title,
        chatMessages = uiState.chatMessages,
        debugView = uiState.debugView,
        inputText = uiState.inputText,
        isInputEnabled = uiState.isInputEnabled,
        isLoading = uiState.isLoading,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentDemoScreenContent(
    title: String,
    chatMessages: List<ChatMessage>,
    debugView: DebugView,
    inputText: String,
    isInputEnabled: Boolean,
    isLoading: Boolean,
    onEvent: (AgentDemoUiEvents) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val visibleMessages = remember(chatMessages, debugView) {
        chatMessages.filter(debugView::shows)
    }

    // Scroll to bottom when messages change
    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(AgentDemoUiEvents.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    DebugViewSelector(
                        modifier = Modifier.padding(end = AppDimension.spacingMedium),
                        debugView = debugView,
                        onDebugViewChanged = { onEvent(AgentDemoUiEvents.UpdateDebugView(it)) }
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimension.spacingMedium),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingMedium)
            ) {
                items(visibleMessages) { message ->
                    when (message) {
                        is ChatMessage.UserMessage -> UserMessageBubble(message.text)
                        is ChatMessage.AgentMessage -> AgentMessageBubble(message.text)
                        is ChatMessage.SystemMessage -> SystemMessageItem(message.text)
                        is ChatMessage.ErrorMessage -> ErrorMessageItem(message.text)
                        is ChatMessage.ToolCallMessage -> ToolCallMessageItem(message.toolName, message.args)
                        is ChatMessage.LLMCallMessage -> LLMCallMessageItem(message.data)
                    }
                }

                // Add extra space at the bottom for better UX
                item {
                    Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
                }
            }

            // Input area
            InputArea(
                text = inputText,
                onTextChanged = { onEvent(AgentDemoUiEvents.UpdateInputText(it)) },
                onSendClicked = {
                    onEvent(AgentDemoUiEvents.SendMessage)
                    focusManager.clearFocus()
                },
                isEnabled = isInputEnabled,
                isLoading = isLoading,
                focusRequester = focusRequester
            )
        }
    }
}

@Composable
private fun DebugViewSelector(
    modifier: Modifier = Modifier,
    debugView: DebugView,
    onDebugViewChanged: (DebugView) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall)
    ) {
        Text(
            text = "Debug View:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DebugView.entries.forEach { option ->
            DebugViewOption(
                label = option.title,
                selected = debugView == option,
                onClick = { onDebugViewChanged(option) }
            )
        }
    }
}

@Composable
private fun DebugViewOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * MAX_BUBBLE_WIDTH_FRACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(RoundedCornerShape(AppDimension.radiusExtraLarge))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(AppDimension.spacingMedium)
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * MAX_BUBBLE_WIDTH_FRACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(RoundedCornerShape(AppDimension.radiusExtraLarge))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(AppDimension.spacingMedium)
            ) {
                Markdown(
                    content = text,
                    colors = markdownColor(text = MaterialTheme.colorScheme.onPrimaryContainer),
                    typography = markdownTypography(text = MaterialTheme.typography.bodyLarge)
                )
            }
        }
    }
}

@Composable
private fun SystemMessageItem(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimension.spacingMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorMessageItem(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * MAX_BUBBLE_WIDTH_FRACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
            ) {
                Text(
                    text = "Error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = AppDimension.spacingSmall)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimension.radiusExtraLarge))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(AppDimension.spacingMedium)
                ) {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolCallMessageItem(toolName: String, args: Map<String, String>) {
    val borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * MAX_BUBBLE_WIDTH_FRACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(modifier = Modifier.widthIn(max = maxBubbleWidth)) {
                Text(
                    text = "TOOL CALL",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = AppDimension.spacingSmall, bottom = 2.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(AppDimension.spacingSmall),
                    horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall)
                ) {
                    // Tool name chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = AppDimension.spacingSmall, vertical = 2.dp)
                    ) {
                        Text(
                            text = toolName,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    // Argument chips
                    args.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = AppDimension.spacingSmall, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = key,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = ": ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                            Text(
                                text = value,
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean,
    focusRequester: FocusRequester
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AppDimension.elevationMedium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimension.spacingMedium,
                    vertical = AppDimension.spacingSmall
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Type a message...") },
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClicked() }),
                singleLine = true,
                shape = RoundedCornerShape(AppDimension.radiusRound)
            )

            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))

            // Send button or loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(AppDimension.iconButtonSizeLarge)
                        .padding(AppDimension.spacingButtonPadding)
                )
            } else {
                IconButton(
                    onClick = onSendClicked,
                    enabled = isEnabled && text.isNotBlank(),
                    modifier = Modifier
                        .size(AppDimension.iconButtonSizeLarge)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled && text.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isEnabled && text.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun AgentDemoScreenPreview() {
    AppTheme {
        AgentDemoScreenContent(
            title = "Agent Demo",
            chatMessages = listOf(
                ChatMessage.SystemMessage("Hi, I'm an agent that can help you"),
                ChatMessage.UserMessage("Hello!"),
                ChatMessage.LLMCallMessage(
                    LlmCallData(
                        messageHistory = listOf(
                            LlmCallHistoryItem.System("You are a helpful weather assistant.\nUse tools when needed."),
                            LlmCallHistoryItem.User("What's the weather in Munich today?"),
                            LlmCallHistoryItem.ToolCall("currentDatetime", """{"timezone":"Europe/Berlin"}"""),
                            LlmCallHistoryItem.ToolResult("currentDatetime", "Current datetime: 2026-04-18T11:36:22+02:00")
                        ),
                        availableTools = listOf(
                            LlmCallToolData("addDatetime", listOf("date", "days", "hours", "minutes"), emptyList()),
                            LlmCallToolData("currentDatetime", listOf("timezone"), emptyList())
                        )
                    )
                ),
                ChatMessage.ToolCallMessage("get_weather", mapOf("location" to "Paris", "date" to "2024-01-15")),
                ChatMessage.AgentMessage("Hello! How can I help you today?"),
                ChatMessage.ErrorMessage("Error: Something went wrong")
            ),
            debugView = DebugView.FullTrace,
            inputText = "",
            isInputEnabled = true,
            isLoading = false,
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun AgentDemoScreenEndedPreview() {
    AppTheme {
        AgentDemoScreenContent(
            title = "Agent Demo",
            chatMessages = listOf(
                ChatMessage.SystemMessage("Hi, I'm an agent that can help you"),
                ChatMessage.UserMessage("Hello!"),
                ChatMessage.AgentMessage("Hello! How can I help you today?"),
                ChatMessage.SystemMessage("The agent has stopped.")
            ),
            debugView = DebugView.Off,
            inputText = "",
            isInputEnabled = false,
            isLoading = false,
            onEvent = {},
        )
    }
}
