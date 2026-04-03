package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.admin.app.AdminRoute
import org.example.project.chat.ChatScreen
import org.example.project.chat.ChatTopBar
import org.example.project.chat.ChatViewModel
import org.example.project.domain.character.Character
import kotlin.uuid.Uuid

fun main() {
    val session = Uuid.random()
    println("Session: $session")

    val dependencies = dependencies()

    application {
        var adminWindowOpen by remember { mutableStateOf(false) }
        var loginDialogOpen by remember { mutableStateOf(false) }
        var loggedInCharacter by remember { mutableStateOf<Character?>(null) }
        var characters by remember { mutableStateOf<List<Character>>(emptyList()) }

        LaunchedEffect(Unit) {
            characters = dependencies.characterService.listCharacters()
        }

        MaterialTheme(AppColorScheme) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Fantasy Store Chat",
                state = rememberWindowState(
                    size = DpSize(1200.dp, 800.dp)
                )
            ) {
                val chatViewModel: ChatViewModel =
                    viewModel(factory = ChatViewModel.factory(session, dependencies.chat))
                val chatUiState by chatViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { chatViewModel.loadHistory() }

                Scaffold(
                    topBar = {
                        ChatTopBar(
                            onAdminClick = { adminWindowOpen = true },
                            onLoginClick = { loginDialogOpen = true },
                            loggedInCharacterName = loggedInCharacter?.name
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        ChatScreen(
                            uiState = chatUiState,
                            onInputChange = chatViewModel::updateInputText,
                            onSendMessage = { chatViewModel.sendMessage(loggedInCharacter?.id) }
                        )
                    }
                }

                if (loginDialogOpen) {
                    AlertDialog(
                        onDismissRequest = { loginDialogOpen = false },
                        title = { Text("Select Character") },
                        text = {
                            Column {
                                if (characters.isEmpty()) {
                                    Text("No characters found.")
                                } else {
                                    LazyColumn {
                                        items(characters) { character ->
                                            TextButton(
                                                onClick = {
                                                    loggedInCharacter = character
                                                    loginDialogOpen = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(character.name)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { loginDialogOpen = false }) {
                                Text("Cancel")
                            }
                        }
                    )
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
                        AdminRoute(dependencies.services)
                    }
                }
            }
        }
    }
}
