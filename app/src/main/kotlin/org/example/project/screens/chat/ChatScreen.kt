package org.example.project.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import org.example.project.AppDimension
import org.example.project.shared.ChatMessage
import org.example.project.shared.ExecutionTraceItem

internal const val MAX_BUBBLE_WIDTH_FRACTION = 0.85f

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    ChatScreenContent(
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
private fun ChatScreenContent(
    title: String,
    chatMessages: List<ChatMessage>,
    debugView: DebugView,
    inputText: String,
    isInputEnabled: Boolean,
    isLoading: Boolean,
    onEvent: (ChatUiEvents) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val visibleMessages = remember(chatMessages, debugView) {
        chatMessages.filter(debugView::shows)
    }

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
                    IconButton(onClick = { onEvent(ChatUiEvents.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ChatUiEvents.RestartChat) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart chat"
                        )
                    }
                    DebugViewSelector(
                        modifier = Modifier.padding(end = AppDimension.spacingMedium),
                        debugView = debugView,
                        onToggleEnabled = { onEvent(ChatUiEvents.ToggleDebugEnabled) },
                        onToggleOption = { onEvent(ChatUiEvents.ToggleDebugOption(it)) },
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
                        is ChatMessage.AskQuestion -> AgentMessageBubble(message.text)
                        is ChatMessage.AgentMessage -> AgentMessageBubble(message.text)
                        is ChatMessage.SystemMessage -> SystemMessageItem(message.text)
                        is ChatMessage.ErrorMessage -> ErrorMessageItem(message.text)
                        is ChatMessage.ToolCallMessage -> ToolCallMessageItem(message.toolName, message.args)
                        is ChatMessage.LLMCallMessage -> LLMCallMessageItem(message.data)
                        is ChatMessage.ExecutionTraceMessage -> ExecutionTraceMessageItem(message.item)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
                }
            }

            InputArea(
                text = inputText,
                onTextChanged = { onEvent(ChatUiEvents.UpdateInputText(it)) },
                onSendClicked = {
                    onEvent(ChatUiEvents.SendMessage)
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
            Column(modifier = Modifier.widthIn(max = maxBubbleWidth)) {
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
private fun ExecutionTraceMessageItem(item: ExecutionTraceItem) {
    when (item) {
        is ExecutionTraceItem.Node -> NodeExecutionTraceItem(item.name)
        is ExecutionTraceItem.Subgraph -> SubgraphExecutionTraceItem(item.name)
    }
}

@Composable
private fun NodeExecutionTraceItem(name: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * MAX_BUBBLE_WIDTH_FRACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "NODE: $name",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .padding(start = AppDimension.spacingSmall, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun SubgraphExecutionTraceItem(name: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * MAX_BUBBLE_WIDTH_FRACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "TASK: $name",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = AppDimension.spacingSmall, vertical = 2.dp)
            )
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
