package org.example.project

import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.koog.ChatAgentProvider
import org.example.project.db.createDataSource
import org.example.project.db.createDatabase
import org.example.project.admin.merchants.AdminMerchantService
import org.example.project.admin.orders.operations.AdminOrderService
import org.example.project.admin.products.AdminProductService
import org.example.project.domain.character.CharacterService
import org.example.project.domain.chat.ChatService
import org.example.project.domain.order.OrderService
import org.example.project.koog.JdbcChatHistoryProvider
import java.lang.System.getenv

fun dependencies(): Dependencies {
    val dataSource = createDataSource()
    val chatHistoryProvider = JdbcChatHistoryProvider(dataSource).also { it.createTable() }
    val database = createDatabase(dataSource)
    val productService = AdminProductService(database)
    val merchantService = AdminMerchantService(database)
    val orderService = AdminOrderService(database)
    val characterService = CharacterService(database)
    val chatService = ChatService(database, chatHistoryProvider)
    val executor = simpleOpenAIExecutor(requireNotNull(getenv("OPENAI_API_KEY")) { "OPENAI_API_KEY not set" })

    val chatAgentProvider = ChatAgentProvider(executor = executor, orderService = OrderService(database))

    val httpClient = HttpClient {
        install(SSE)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    return Dependencies(
        storeServices = Dependencies.StoreServices(productService, merchantService, orderService),
        characterServices = Dependencies.CharacterServices(characterService, chatService),
        chatAgentProvider = chatAgentProvider,
        chatHistoryProvider = chatHistoryProvider,
        httpClient = httpClient,
    )
}

class Dependencies(
    val storeServices: StoreServices,
    val characterServices: CharacterServices,
    val chatAgentProvider: ChatAgentProvider,
    val chatHistoryProvider: JdbcChatHistoryProvider,
    val httpClient: HttpClient,
) {
    class StoreServices(
        val productService: AdminProductService,
        val merchantService: AdminMerchantService,
        val orderService: AdminOrderService,
    )

    class CharacterServices(
        val characterService: CharacterService,
        val chatService: ChatService,
    )
}
