package org.example.project

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
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
import aws.smithy.kotlin.runtime.io.closeIfCloseable
import org.example.project.admin.app.AdminRoute
import org.example.project.admin.data.createDatabase
import org.example.project.admin.data.createDataSource
import org.example.project.chat.ChatAgent
import org.example.project.chat.ChatScreen
import org.example.project.chat.ChatTopBar
import org.example.project.chat.ChatViewModel
import org.example.project.domain.admin.MerchantService
import org.example.project.domain.admin.OrderService
import org.example.project.domain.admin.ProductService
import org.example.project.koog.JdbcChatHistoryProvider
import java.lang.System.getenv
import javax.sql.DataSource


fun main() {
    val session = "single-chat-4"//Uuid.random().toString()
    val dependencies = dependencies()

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
                val chatViewModel: ChatViewModel =
                    viewModel(factory = ChatViewModel.factory(session, dependencies.chat))
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
                        AdminRoute(dependencies.services)
                    }
                }
            }
        }
    }
}
