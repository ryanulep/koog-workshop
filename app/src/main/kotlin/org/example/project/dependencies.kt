package org.example.project

import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.db.createDataSource
import org.example.project.db.createDatabase
import org.example.project.admin.dashboard.AdminDashboardService
import org.example.project.admin.merchants.AdminMerchantService
import org.example.project.admin.orders.operations.AdminOrderService
import org.example.project.admin.products.AdminProductService
import org.example.project.domain.character.CharacterService
import org.example.project.domain.chat.ChatService

fun dependencies(): Dependencies {
    val dataSource = createDataSource()
    val database = createDatabase(dataSource)
    val httpClient = HttpClient {
        install(SSE)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    val productService = AdminProductService(database)
    val merchantService = AdminMerchantService(httpClient)
    val orderService = AdminOrderService(database)
    val dashboardService = AdminDashboardService(httpClient)
    val characterService = CharacterService(httpClient)
    val chatService = ChatService(httpClient)

    return Dependencies(
        storeServices = Dependencies.StoreServices(productService, merchantService, orderService, dashboardService),
        characterServices = Dependencies.CharacterServices(characterService, chatService),
        httpClient = httpClient,
    )
}

class Dependencies(
    val storeServices: StoreServices,
    val characterServices: CharacterServices,
    val httpClient: HttpClient,
) {
    class StoreServices(
        val productService: AdminProductService,
        val merchantService: AdminMerchantService,
        val orderService: AdminOrderService,
        val dashboardService: AdminDashboardService,
    )

    class CharacterServices(
        val characterService: CharacterService,
        val chatService: ChatService,
    )
}
