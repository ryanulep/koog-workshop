package com.jetbrains.koog.workshop.screens.agentdemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jetbrains.koog.workshop.theme.AppDimension
import com.jetbrains.koog.workshop.theme.AppTheme
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import koog_workshop.intro.generated.resources.Res
import koog_workshop.intro.generated.resources.user
import koog_workshop.intro.generated.resources.weatherAgent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal const val MAX_BUBBLE_WIDTH_FRACTION = 0.85f

@Composable
fun AgentDemoScreen(viewModel: AgentDemoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    AgentDemoScreenContent(
        title = uiState.title,
        agentAvatarRes = uiState.agentAvatarRes,
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
    agentAvatarRes: DrawableResource?,
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
                        onToggleEnabled = { onEvent(AgentDemoUiEvents.ToggleDebugEnabled) },
                        onToggleOption = { onEvent(AgentDemoUiEvents.ToggleDebugOption(it)) },
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
                        is ChatMessage.AgentMessage -> AgentMessageBubble(message.text, agentAvatarRes)
                        is ChatMessage.ResultMessage -> AgentMessageBubble(message.text, agentAvatarRes)
                        is ChatMessage.SystemMessage -> SystemMessageItem(message.text)
                        is ChatMessage.ErrorMessage -> ErrorMessageItem(message.text)
                        is ChatMessage.ToolCallMessage -> ToolCallMessageItem(message.toolName, message.args)
                        is ChatMessage.LLMCallMessage -> LLMCallMessageItem(message.data)
                        is ChatMessage.ExecutionTraceMessage -> ExecutionTraceMessageItem(message.item)
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
    onToggleEnabled: () -> Unit,
    onToggleOption: (DebugOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall)
    ) {
        Text(
            text = "Debug:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = debugView.enabled,
            onCheckedChange = { onToggleEnabled() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedBorderColor = MaterialTheme.colorScheme.secondary,
            ),
        )
        Box {
            TextButton(
                onClick = { expanded = true },
                enabled = debugView.enabled,
            ) {
                Text(
                    text = "View",
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DebugOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.title) },
                        onClick = { onToggleOption(option) },
                        leadingIcon = {
                            Checkbox(
                                checked = option in debugView.options,
                                onCheckedChange = null,
                            )
                        },
                    )
                }
            }
        }
    }
}


@Composable
private fun UserMessageBubble(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(Res.drawable.user),
                contentDescription = "User",
                modifier = Modifier
                    .size(AppDimension.messageTitleColumnWidth),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
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
private fun AgentMessageBubble(text: String, avatarRes: DrawableResource?) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val avatarSpace = if (avatarRes != null) AppDimension.messageTitleColumnWidth + AppDimension.spacingSmall else 0.dp
        val maxBubbleWidth = maxWidth - avatarSpace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (avatarRes != null) {
                Image(
                    painter = painterResource(avatarRes),
                    contentDescription = "Agent",
                    modifier = Modifier
                        .size(AppDimension.messageTitleColumnWidth),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            }
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
        val maxBubbleWidth = maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(AppDimension.messageTitleColumnWidth),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = AppDimension.spacingMedium)
                )
            }
            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolCallMessageItem(toolName: String, args: Map<String, String>) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxContentWidth = maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(AppDimension.messageTitleColumnWidth),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Tool\nCall",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            FlowRow(
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(AppDimension.spacingSmall),
                horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = AppDimension.spacingSmall, vertical = 2.dp)
                ) {
                    Text(
                        text = toolName,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
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
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutionTraceMessageItem(item: ExecutionTraceItem) {
    when (item) {
        is ExecutionTraceItem.Node -> NodeExecutionTraceItem(item.name)
        is ExecutionTraceItem.SubgraphStarted -> SubgraphStartedTraceItem(item.name)
        is ExecutionTraceItem.SubgraphCompleted -> SubgraphCompletedTraceItem(item.name, item.result)
    }
}

@Composable
private fun NodeExecutionTraceItem(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(AppDimension.messageTitleColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Node",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
        Text(
            text = name,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SubgraphStartedTraceItem(name: String) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(AppDimension.messageTitleColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Task\nStart",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(AppDimension.spacingSmall)
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun SubgraphCompletedTraceItem(name: String, result: String?) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(AppDimension.messageTitleColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Task\nResult",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(AppDimension.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimension.spacingExtraSmall)
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            if (!result.isNullOrBlank()) {
                Text(
                    text = result,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
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
            agentAvatarRes = Res.drawable.weatherAgent,
            chatMessages = listOf(
                ChatMessage.SystemMessage("Hi, I'm an agent that can help you"),
                ChatMessage.UserMessage("Hello!"),
                ChatMessage.LLMCallMessage(
                    LlmCallData(
                        messageHistory = listOf(
                            LlmCallHistoryItem.System("You are a helpful weather assistant.\nUse tools when needed."),
                            LlmCallHistoryItem.User("What's the weather in Munich today?"),
                            LlmCallHistoryItem.ToolCall("currentDatetime", """{"timezone":"Europe/Berlin"}"""),
                            LlmCallHistoryItem.ToolResult(
                                "currentDatetime",
                                "Current datetime: 2026-04-18T11:36:22+02:00"
                            )
                        ),
                        availableTools = listOf(
                            LlmCallToolData("addDatetime", listOf("date", "days", "hours", "minutes"), emptyList()),
                            LlmCallToolData("currentDatetime", listOf("timezone"), emptyList())
                        )
                    )
                ),
                ChatMessage.ToolCallMessage("get_weather", mapOf("location" to "Paris", "date" to "2024-01-15")),
                ChatMessage.ExecutionTraceMessage(ExecutionTraceItem.Node("nodeCallLLM")),
                ChatMessage.ExecutionTraceMessage(ExecutionTraceItem.SubgraphStarted("assess")),
                ChatMessage.ExecutionTraceMessage(ExecutionTraceItem.SubgraphCompleted("assess", "done")),
                ChatMessage.AgentMessage("Hello! How can I help you today?"),
                ChatMessage.ErrorMessage("Error: Something went wrong")
            ),
            debugView = DebugView(enabled = true),
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
            agentAvatarRes = Res.drawable.weatherAgent,
            chatMessages = listOf(
                ChatMessage.SystemMessage("Hi, I'm an agent that can help you"),
                ChatMessage.UserMessage("Hello!"),
                ChatMessage.AgentMessage("Hello! How can I help you today?"),
                ChatMessage.SystemMessage("The agent has stopped.")
            ),
            debugView = DebugView(),
            inputText = "",
            isInputEnabled = false,
            isLoading = false,
            onEvent = {},
        )
    }
}
