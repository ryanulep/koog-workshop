package org.example.project.db

import ai.koog.agents.features.chathistory.jdbc.JdbcChatHistoryProvider
import ai.koog.agents.features.chatmemory.sql.SQLChatHistorySchemaMigrator
import kotlinx.serialization.json.Json
import javax.sql.DataSource

class SqliteJdbcChatHistoryProvider @JvmOverloads constructor(
    dataSource: DataSource,
    tableName: String = "chat_history",
    ttlSeconds: Long? = null,
    migrator: SQLChatHistorySchemaMigrator = SqliteJdbcChatHistorySchemaMigrator(dataSource, tableName),
    json: Json = Json { ignoreUnknownKeys = true }
) : JdbcChatHistoryProvider(dataSource, migrator, ttlSeconds, tableName) {
    override val upsertSql: String = """
        INSERT INTO $tableName (conversation_id, messages_json, updated_at, ttl_timestamp)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(conversation_id) DO UPDATE SET
            messages_json = excluded.messages_json,
            updated_at = excluded.updated_at,
            ttl_timestamp = excluded.ttl_timestamp
    """.trimIndent()
}

public class SqliteJdbcChatHistorySchemaMigrator @JvmOverloads constructor(
    private val dataSource: DataSource,
    private val tableName: String = "chat_history"
) : SQLChatHistorySchemaMigrator {
    override suspend fun migrate() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS $tableName (
                        conversation_id VARCHAR(255) NOT NULL,
                        messages_json TEXT NOT NULL,
                        updated_at BIGINT NOT NULL DEFAULT 0,
                        ttl_timestamp BIGINT NULL,
                        PRIMARY KEY (conversation_id)
                    )
                    """.trimIndent()
                )

                val columns = mutableListOf<String>()
                connection.prepareStatement("PRAGMA table_info($tableName)").executeQuery().use { rs ->
                    while (rs.next()) {
                        columns.add(rs.getString("name"))
                    }
                }

                if (!columns.contains("updated_at")) {
                    stmt.execute("ALTER TABLE $tableName ADD COLUMN updated_at BIGINT NOT NULL DEFAULT 0")
                }
                if (!columns.contains("ttl_timestamp")) {
                    stmt.execute("ALTER TABLE $tableName ADD COLUMN ttl_timestamp BIGINT NULL")
                }
                if (columns.contains("id") && !columns.contains("conversation_id")) {
                    // Simple migration for the case found in the DB
                    stmt.execute("ALTER TABLE $tableName RENAME COLUMN id TO conversation_id")
                }
                if (columns.contains("messages") && !columns.contains("messages_json")) {
                    stmt.execute("ALTER TABLE $tableName RENAME COLUMN messages TO messages_json")
                }

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_${tableName}_updated_at ON $tableName (updated_at)")
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_${tableName}_ttl_timestamp ON $tableName (ttl_timestamp)")
            }
        }
    }
}
