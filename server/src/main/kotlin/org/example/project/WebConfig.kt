package org.example.project

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.features.chatmemory.sql.migrateBlocking
import ai.koog.agents.features.persistence.jdbc.JdbcPersistenceStorageProvider
import org.example.project.db.SqliteJdbcChatHistoryProvider
import org.example.project.db.SqliteJdbcPersistenceStorageProvider
import org.example.project.domain.shared.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.sql.DataSource
import kotlin.uuid.Uuid

@Configuration
class WebConfig : WebMvcConfigurer {
    @Bean
    fun kotlinSerializationJsonHttpMessageConverter(): KotlinSerializationJsonHttpMessageConverter =
        KotlinSerializationJsonHttpMessageConverter()

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
    fun chatHistory(dataSource: DataSource): ChatHistoryProvider =
        SqliteJdbcChatHistoryProvider(dataSource).also { it.migrateBlocking() }

    @Bean
    fun persistence(dataSource: DataSource): JdbcPersistenceStorageProvider =
        SqliteJdbcPersistenceStorageProvider(dataSource).also { it.migrateBlocking() }
}
