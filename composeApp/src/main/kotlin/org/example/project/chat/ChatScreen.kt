package org.example.project.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun ChatScreen(
    uiState: ChatUi,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MessageList(
            messages = uiState.messages,
            modifier = Modifier.weight(1f)
        )

        InputArea(
            inputText = uiState.inputText,
            isSending = uiState.isSending,
            onInputChange = onInputChange,
            onSendMessage = onSendMessage
        )
    }
}

@Composable
private fun MessageList(
    messages: List<ChatUi.Message>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatUi.Message) {
    val arrangement = when (message) {
        is ChatUi.Message.User -> Arrangement.End
        is ChatUi.Message.CustomerSupport -> Arrangement.Start
    }
    val bubbleColor = when (message) {
        is ChatUi.Message.User -> MaterialTheme.colorScheme.primaryContainer
        is ChatUi.Message.CustomerSupport -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when (message) {
        is ChatUi.Message.User -> MaterialTheme.colorScheme.onPrimaryContainer
        is ChatUi.Message.CustomerSupport -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

@Composable
private fun InputArea(
    inputText: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = !isSending,
                maxLines = 3
            )

            Button(
                onClick = onSendMessage,
                enabled = !isSending && inputText.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Instant): String {
    val localDateTime = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    return String.format("%02d:%02d", localDateTime.hour, localDateTime.minute)
}
