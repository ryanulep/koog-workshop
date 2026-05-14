package org.example.project.admin.orders.operations

import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/orders")
class AdminOrderController(
    private val adminOrderService: AdminOrderService
) {
    @GetMapping("/merchants")
    suspend fun getMerchantOptions(): List<OrderMerchantOption> {
        return adminOrderService.loadMerchantOptions()
    }

    @PostMapping("/list")
    suspend fun getOrders(@RequestBody filter: OrderFilter): List<OrderListItem> {
        return adminOrderService.loadOrders(filter)
    }

    @GetMapping("/{orderId}")
    suspend fun getOrderDetail(@PathVariable orderId: OrderId): AdminOrderDetail? {
        return adminOrderService.loadOrderDetailOrNull(orderId)
    }

    @PatchMapping("/sub-orders/{subOrderId}/status")
    suspend fun updateSubOrderStatus(
        @PathVariable subOrderId: SubOrderId,
        @RequestBody request: UpdateOrderStatusRequest
    ): Boolean {
        return adminOrderService.updateSubOrderStatus(subOrderId, request.status)
    }

    @PatchMapping("/{orderId}/status")
    suspend fun updateOrderStatus(
        @PathVariable orderId: OrderId,
        @RequestBody request: UpdateOrderStatusRequest
    ): Boolean {
        return adminOrderService.updateOrderStatus(orderId, request.status)
    }
}
