package org.example.project.service

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import kotlinx.coroutines.test.runTest
import org.example.project.db.connectSqlite
import org.example.project.db.createDataSource
import org.example.project.db.createTables
import org.example.project.domain.chat.ChatService
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.JdbcChatHistoryProvider
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class ChatServiceTest {
    private lateinit var testDbFile: Path
    private lateinit var database: Database
    private lateinit var chatHistoryProvider: ChatHistoryProvider
    private lateinit var chatService: ChatService

    @BeforeTest
    fun setup() {
        testDbFile = Files.createTempFile("test_chat_", ".db")
        database = connectSqlite(testDbFile).createTables()
        chatHistoryProvider = JdbcChatHistoryProvider(createDataSource(testDbFile))
        chatService = ChatService(database, chatHistoryProvider)
    }

    @AfterTest
    fun tearDown() {
        testDbFile.deleteIfExists()
    }

    @Test
    fun testChatService() = runTest {
        val result = chatService.getCharacterChatDetails(CharacterId(Uuid.generateV7()))
        assertTrue { result.isEmpty() }
    }
}