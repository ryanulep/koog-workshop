package org.example.project.screens.chatlist

import ai.koog.prompt.message.Message
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.project.AppDimension
import org.example.project.domain.chat.ChatDetails
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatListScreen(viewModel: ChatListViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChats()
    }

    ChatListContent(
        characterName = uiState.character.name,
        chats = uiState.chats,
        isLoading = uiState.isLoading,
        onChatSelected = viewModel::selectChat,
        onNewChat = viewModel::startNewChat,
        onNavigateBack = viewModel::navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListContent(
    characterName: String,
    chats: List<ChatDetails>,
    isLoading: Boolean,
    onChatSelected: (ChatDetails) -> Unit,
    onNewChat: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(characterName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNewChat) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            chats.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppDimension.spacingMedium)
                ) {
                    Text("No chats yet", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onNewChat) { Text("Start a new chat") }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(AppDimension.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall)
            ) {
                items(chats, key = { it.conversationId }) { chat ->
                    ChatItem(chat = chat, onClick = { onChatSelected(chat) })
                }
            }
        }
    }
}

@Composable
private fun ChatItem(chat: ChatDetails, onClick: () -> Unit) {
    val lastVisibleMessage = chat.messages.lastOrNull { it is Message.User || it is Message.Assistant }
    val preview = when (lastVisibleMessage) {
        is Message.User -> lastVisibleMessage.content
        is Message.Assistant -> lastVisibleMessage.content
        else -> "No messages"
    }
    val timeText = lastVisibleMessage?.metaInfo?.timestamp?.let { timestamp ->
        val javaInstant = Instant.ofEpochMilli(timestamp.toEpochMilliseconds())
        val zone = ZoneId.systemDefault()
        val dateTime = LocalDateTime.ofInstant(javaInstant, zone)
        val today = LocalDate.now(zone)
        if (dateTime.toLocalDate() == today) {
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            dateTime.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } ?: ""

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimension.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.conversationId,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (timeText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
