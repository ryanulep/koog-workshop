package org.example.project

import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import org.example.project.admin.data.createDataSource
import org.example.project.chat.ChatAgent
import org.example.project.chat.ChatScreen
import org.example.project.chat.ChatTopBar
import org.example.project.chat.ChatViewModel
import org.example.project.koog.JdbcChatHistoryProvider
import java.lang.System.getenv

fun main() {
    val session = "single-chat-3"//Uuid.random().toString()
    val dataSource = createDataSource()
    val history = JdbcChatHistoryProvider(dataSource).also { it.createTable() }
    val executor = simpleOpenAIExecutor(requireNotNull(getenv("OPENAI_API_KEY")) { "OPENAI_API_KEY not set" })
    val chat = ChatAgent(executor = executor, history = history, sessionId = session)

    application {
        var adminWindowOpen by remember { mutableStateOf(false) }

        MaterialTheme(AppColorScheme) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Fantasy Store Chat",
                state = rememberWindowState(
                    size = DpSize(1200.dp, 800.dp)
                )
            ) {
                val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(session, chat))
                val chatUiState by chatViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { chatViewModel.loadHistory() }

                Scaffold(
                    topBar = {
                        ChatTopBar(onAdminClick = { adminWindowOpen = true })
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        ChatScreen(
                            uiState = chatUiState,
                            onInputChange = chatViewModel::updateInputText,
                            onSendMessage = chatViewModel::sendMessage
                        )
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
                        AdminApp()
                    }
                }
            }
        }
    }
}
