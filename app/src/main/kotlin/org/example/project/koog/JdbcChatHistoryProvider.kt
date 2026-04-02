package org.example.project.koog

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.prompt.message.Message
import kotlinx.serialization.json.Json
import javax.sql.DataSource

class JdbcChatHistoryProvider(private val source: DataSource) : ChatHistoryProvider {
    fun createTable() {
        source.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS chat_history (id VARCHAR(255) PRIMARY KEY, messages TEXT)")
            }
        }
    }

    override suspend fun store(conversationId: String, messages: List<Message>) {
        source.connection.use { conn ->
            conn.prepareStatement("INSERT OR REPLACE INTO chat_history (id, messages) VALUES (?, ?)").use { stmt ->
                stmt.setString(1, conversationId)
                stmt.setString(2, Json.encodeToString(messages))
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun load(conversationId: String): List<Message> =
        source.connection.use { conn ->
            conn.prepareStatement("SELECT messages FROM chat_history WHERE id = ?").use { stmt ->
                stmt.setString(1, conversationId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val messages = rs.getString("messages")
                        Json.Default.decodeFromString(messages)
                    } else emptyList()
                }
            }
        }
}