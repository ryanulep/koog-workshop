package org.example.project

import ai.koog.prompt.message.Message
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.admin.app.AdminRoute
import org.example.project.screens.chatlist.ChatListScreen
import org.example.project.screens.chatlist.ChatListViewModel
import org.example.project.screens.chat.ChatScreen
import org.example.project.screens.chat.ChatViewModel
import org.example.project.domain.character.Character
import org.example.project.screens.login.LoginScreen

private sealed interface Screen {
    @Immutable
    data object Login : Screen

    @Immutable
    data class ChatList(val character: Character) : Screen

    @Immutable
    data class Chat(
        val character: Character,
        val conversationId: String,
        val initialMessages: List<Message>?,
    ) : Screen
}

fun main() {
    val dependencies = dependencies()

    application {
        var adminWindowOpen by remember { mutableStateOf(false) }
        var screen by remember { mutableStateOf<Screen>(Screen.Login) }
        var characters by remember { mutableStateOf<List<Character>>(emptyList()) }

        LaunchedEffect(Unit) {
            characters = dependencies.characterServices.characterService.listCharacters()
        }

        MaterialTheme(AppColorScheme) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Fantasy Store Chat",
                state = rememberWindowState(
                    size = DpSize(1200.dp, 800.dp)
                )
            ) {
                when (val current = screen) {
                    is Screen.Login -> {
                        LoginScreen(
                            characters = characters,
                            onAdminClick = { adminWindowOpen = true },
                            onCharacterSelected = { screen = Screen.ChatList(it) }
                        )
                    }

                    is Screen.ChatList -> {
                        val chatListViewModel: ChatListViewModel = viewModel(
                            key = "chatlist-${current.character.id.value}",
                            factory = ChatListViewModel.factory(
                                character = current.character,
                                chatService = dependencies.characterServices.chatService,
                                onNavigateBack = { screen = Screen.Login },
                                onChatSelected = { conversationId, messages ->
                                    screen = Screen.Chat(current.character, conversationId, messages)
                                },
                                onNewChat = { conversationId ->
                                    screen = Screen.Chat(current.character, conversationId, null)
                                },
                            )
                        )
                        ChatListScreen(viewModel = chatListViewModel)
                    }

                    is Screen.Chat -> {
                        val chatViewModel: ChatViewModel = viewModel(
                            key = current.conversationId,
                            factory = ChatViewModel.factory(
                                character = current.character,
                                conversationId = current.conversationId,
                                initialMessages = current.initialMessages,
                                chatService = dependencies.characterServices.chatService,
                                httpClient = dependencies.httpClient,
                                onNavigateBack = { screen = Screen.ChatList(current.character) },
                            )
                        )
                        ChatScreen(viewModel = chatViewModel)
                    }
                }

                if (adminWindowOpen) {
                    Window(
                        onCloseRequest = { adminWindowOpen = false },
                        title = "Fantasy Store Admin",
                        state = rememberWindowState(
                            size = DpSize(1500.dp, 800.dp),
                            position = WindowPosition(100.dp, 100.dp)
                        )
                    ) {
                        AdminRoute(dependencies.storeServices)
                    }
                }
            }
        }
    }
}
