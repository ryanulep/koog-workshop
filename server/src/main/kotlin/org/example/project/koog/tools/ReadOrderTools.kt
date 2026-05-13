package org.example.project.koog.tools

import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import org.example.project.domain.order.OrderService
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.OrderId
import org.example.project.koog.OrderDetails
import kotlin.uuid.Uuid

class ReadOrderTools(
    val characterId: CharacterId,
    val orderService: OrderService
) : ToolSet {
    // FIXME Add LLM descriptions for tools and arguments
    @Tool
    suspend fun getOrderHistory(offset: Long = 0, limit: Long = 5): List<OrderDetails> =
        orderService.getOrderHistory(characterId)
            .items.map {
                OrderDetails(
                    it.id.value.toString(),
                    it.status,
                    it.createdAt,
                    it.updatedAt
                )
            }

    @Tool
    suspend fun getOrderOrNull(orderId: String): OrderDetails? =
        orderService.getOrderDetailsOrNull(OrderId(Uuid.parse(orderId)))
            ?.let {
                OrderDetails(
                    it.order.id.value.toString(),
                    it.order.status,
                    it.order.createdAt,
                    it.order.updatedAt
                )
            }
}
