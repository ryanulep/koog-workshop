package org.example.project.admin.orders.operations


import org.example.project.domain.order.OrderRepository
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class AdminOrderService(
    private val adminOrderRepository: AdminOrderRepository,
    private val orderRepository: OrderRepository
) {
    fun loadMerchantOptions(): List<OrderMerchantOption> =
        adminOrderRepository.getMerchantOptions()

    fun loadOrders(filter: OrderFilter): List<OrderListItem> =
        adminOrderRepository.getOrders(filter)

    fun loadOrderDetailOrNull(orderId: OrderId): AdminOrderDetail? =
        adminOrderRepository.getOrderDetailOrNull(orderId)

    fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean =
        orderRepository.updateSubOrderStatus(subOrderId, status)

    fun updateOrderStatus(orderId: OrderId, status: OrderStatus): Boolean =
        orderRepository.updateOrderStatus(orderId, status)
}
