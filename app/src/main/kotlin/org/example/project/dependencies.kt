package org.example.project

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.admin.merchants.AdminMerchantService
import org.example.project.admin.orders.operations.AdminOrderService
import org.example.project.admin.products.AdminProductService
import org.example.project.screens.chat.ChatService
import org.example.project.screens.chatlist.CharacterService

fun dependencies(): Dependencies {
    val httpClient = HttpClient {
        install(SSE)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    val productService = AdminProductService(httpClient)
    val merchantService = AdminMerchantService(httpClient)
    val adminOrderService = AdminOrderService(httpClient)
    val characterService = CharacterService(httpClient)
    val chatService = ChatService(httpClient)

    return Dependencies(
        storeServices = Dependencies.StoreServices(productService, merchantService, adminOrderService),
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
        val adminOrderService: AdminOrderService,
    )

    class CharacterServices(
        val characterService: CharacterService,
        val chatService: ChatService,
    )
}
