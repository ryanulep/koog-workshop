package org.example.project

import ai.koog.agents.features.persistence.jdbc.JdbcPersistenceStorageProvider
import ai.koog.agents.features.persistence.jdbc.PostgresJdbcPersistenceSchemaMigrator
import ai.koog.agents.features.sql.providers.SQLPersistenceSchemaMigrator
import kotlinx.serialization.json.Json
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.core.Table
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.UUID
import javax.sql.DataSource
import kotlin.uuid.Uuid

@Configuration
class WebConfig : WebMvcConfigurer {
    @Bean
    fun kotlinSerializationJsonHttpMessageConverter(): KotlinSerializationJsonHttpMessageConverter {
        return KotlinSerializationJsonHttpMessageConverter()
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(String::class.java, Uuid::class.java) { Uuid.parse(it) }
        registry.addConverter(String::class.java, OrderId::class.java) { OrderId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, SubOrderId::class.java) { SubOrderId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, CharacterId::class.java) { CharacterId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, ProductId::class.java) { ProductId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, MerchantId::class.java) { MerchantId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, ShippingMethodId::class.java) { ShippingMethodId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, TransactionId::class.java) { TransactionId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, CurrencyId::class.java) { CurrencyId(Uuid.parse(it)) }
    }

    @Bean
    fun persistence(dataSource: DataSource): JdbcPersistenceStorageProvider =
        SqlliteJdbcPersistenceStorageProvider(dataSource)
}

public class SqlliteJdbcPersistenceStorageProvider @JvmOverloads constructor(
    dataSource: DataSource,
    tableName: String = "agent_checkpoints",
    ttlSeconds: Long? = null,
    migrator: SQLPersistenceSchemaMigrator = PostgresJdbcPersistenceSchemaMigrator(dataSource, tableName),
) : JdbcPersistenceStorageProvider(dataSource, migrator, ttlSeconds, tableName) {
    override val upsertSql: String = """
        INSERT INTO $tableName (
            persistence_id,
            checkpoint_id,
            created_at,
            checkpoint_json,
            ttl_timestamp,
            version
        )
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(persistence_id, checkpoint_id) DO UPDATE SET
            created_at = excluded.created_at,
            checkpoint_json = excluded.checkpoint_json,
            ttl_timestamp = excluded.ttl_timestamp,
            version = excluded.version
    """.trimIndent()
}

object AgentCheckpoints : Table("agent_checkpoints") {
    val persistenceId = text("persistence_id")
    val checkpointId = text("checkpoint_id")
    val createdAt = long("created_at")
    val checkpointJson = text("checkpoint_json")
    val ttlTimestamp = long("ttl_timestamp").nullable()
    val version = long("version")

    override val primaryKey = PrimaryKey(persistenceId, checkpointId)
}
