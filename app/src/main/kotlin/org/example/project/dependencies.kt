package org.example.project

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import org.example.project.admin.data.createDataSource
import org.example.project.admin.data.createDatabase
import org.example.project.chat.ChatAgent
import org.example.project.domain.admin.MerchantService
import org.example.project.domain.admin.ProductService
import org.example.project.domain.order.OrderService
import org.example.project.koog.JdbcChatHistoryProvider
import java.lang.System.getenv

fun dependencies(): Dependencies {
    val dataSource = createDataSource()
    val chatProvider = JdbcChatHistoryProvider(dataSource).also { it.createTable() }
    val database = createDatabase(dataSource)
    val productService = ProductService(database)
    val merchantService = MerchantService(database)
    val orderService = OrderService(database)
    val executor = simpleOpenAIExecutor(requireNotNull(getenv("OPENAI_API_KEY")) { "OPENAI_API_KEY not set" })
    val chat = ChatAgent(executor = executor, history = chatProvider)
    return Dependencies(Dependencies.Services(productService, merchantService, orderService), chat)
}

class Dependencies(
    val services: Services,
    val chat: ChatAgent
) {
    class Services(
        val productService: ProductService,
        val merchantService: MerchantService,
        val orderService: OrderService
    )
}
