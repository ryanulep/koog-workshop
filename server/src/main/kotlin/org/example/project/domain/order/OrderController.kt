package org.example.project.domain.order

import org.example.project.domain.shared.*
import org.springframework.web.bind.annotation.*
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping("/checkout/{characterId}")
    suspend fun checkout(
        @PathVariable characterId: String,
        @RequestBody request: CheckoutRequest
    ): OrderId {
        val mappedSelections = request.shippingSelections.map { (merchantId, shippingMethodId) ->
            MerchantId(Uuid.parse(merchantId)) to ShippingMethodId(Uuid.parse(shippingMethodId))
        }.toMap()
        return orderService.checkout(CharacterId(Uuid.parse(characterId)), mappedSelections)
    }

    @GetMapping("/{id}")
    suspend fun getOrder(@PathVariable id: String): Order? {
        return orderService.getOrderOrNull(OrderId(Uuid.parse(id)))
    }

    @GetMapping("/history/{characterId}")
    suspend fun getOrderHistory(
        @PathVariable characterId: String,
        @RequestParam(defaultValue = "0") offset: Long,
        @RequestParam(defaultValue = "50") limit: Long
    ): Page<Order> {
        return orderService.getOrderHistory(CharacterId(Uuid.parse(characterId)), offset, limit)
    }

    @GetMapping("/{id}/details")
    suspend fun getOrderDetails(@PathVariable id: String): OrderDetails? {
        return orderService.getOrderDetailsOrNull(OrderId(Uuid.parse(id)))
    }

    @PostMapping("/{id}/cancel")
    suspend fun cancelOrder(@PathVariable id: String): Boolean {
        return orderService.cancelOrder(OrderId(Uuid.parse(id)))
    }
}
