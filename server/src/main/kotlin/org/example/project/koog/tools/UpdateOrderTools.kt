package org.example.project.koog.tools

import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import org.example.project.domain.order.OrderService
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId
import kotlin.uuid.Uuid

class UpdateOrderTools(
    val characterId: CharacterId,
    val orderService: OrderService
) : ToolSet {
    // TODO: update cancelOrder such that it guarantees that its characterId is the owner of the order??
    // FIXME add LLM descriptions for tools and arguments
    @Tool
    suspend fun cancelOrder(orderId: String) = orderService.cancelOrder(OrderId(Uuid.parse(orderId)))

    @Tool
    suspend fun updateSubOrderStatus(subOrderId: String, status: OrderStatus) =
        orderService.updateSubOrderStatus(SubOrderId(Uuid.parse(subOrderId)), status)
}