package org.example.project.service

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import kotlinx.coroutines.test.runTest
import org.example.project.db.connectSqlite
import org.example.project.db.createDataSource
import org.example.project.db.createTables
import org.example.project.db.suspendTransaction
import org.example.project.domain.character.CharacterService
import org.example.project.domain.character.Characters
import org.example.project.domain.chat.ChatService
import org.example.project.domain.chat.ChatUpdate
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.JdbcChatHistoryProvider
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class ChatServiceTest {
    private lateinit var testDbFile: Path
    private lateinit var database: Database
    private lateinit var chatHistoryProvider: ChatHistoryProvider
    private lateinit var chatService: ChatService
    private lateinit var characterService: CharacterService

    @BeforeTest
    fun setup() {
        testDbFile = Files.createTempFile("test_chat_", ".db")
        database = connectSqlite(testDbFile).createTables()

        chatHistoryProvider = JdbcChatHistoryProvider(createDataSource(testDbFile))
            .also { it.createTable() }

        chatService = ChatService(database, chatHistoryProvider)
        characterService = CharacterService(database)
    }

    @AfterTest
    fun tearDown() {
        testDbFile.deleteIfExists()
    }

    @Test
    fun testUpdateChatCreatesRetrievableChatWithHistory() = runTest {
        val characterId = characterService.createCharacter("Gandalf")
        val conversationId = "conv-1"
        val messages = listOf(
            Message.User("hello", RequestMetaInfo.Empty),
            Message.Assistant("hi there", ResponseMetaInfo.Empty),
        )
        chatHistoryProvider.store(conversationId, messages)

        chatService.updateChat(ChatUpdate(characterId, conversationId))

        val details = chatService.getCharacterChatDetails(characterId)
        assertEquals(1, details.size)
        assertEquals(conversationId, details.single().conversationId)
        assertEquals(characterId, details.single().characterId)
        assertEquals(messages, details.single().messages)
    }

    @Test
    fun testUpdateChatIsIdempotentForSameConversation() = runTest {
        val characterId = characterService.createCharacter("Gandalf")
        val conversationId = "conv-1"

        chatService.updateChat(ChatUpdate(characterId, conversationId))
        chatService.updateChat(ChatUpdate(characterId, conversationId))

        val details = chatService.getCharacterChatDetails(characterId)
        assertEquals(1, details.size)
    }

    @Test
    fun testGetCharacterChatDetailsFiltersByCharacter() = runTest {
        val gandalf = characterService.createCharacter("Gandalf")
        val frodo = characterService.createCharacter("Frodo")
        chatService.updateChat(ChatUpdate(gandalf, "conv-gandalf"))
        chatService.updateChat(ChatUpdate(frodo, "conv-frodo-1"))
        chatService.updateChat(ChatUpdate(frodo, "conv-frodo-2"))

        val frodoChats = chatService.getCharacterChatDetails(frodo)
        assertEquals(setOf("conv-frodo-1", "conv-frodo-2"), frodoChats.map { it.conversationId }.toSet())
        assertTrue { frodoChats.all { it.characterId == frodo } }
    }
}
