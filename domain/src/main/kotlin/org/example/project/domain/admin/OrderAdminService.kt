package org.example.project.domain.admin

import org.example.project.db.suspendTransaction
import org.example.project.domain.order.OrderRepository
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId
import org.jetbrains.exposed.v1.jdbc.Database

class OrderAdminService(
    private val database: Database,
    private val adminOrderRepository: AdminOrderRepository = AdminOrderRepository(),
    private val orderRepository: OrderRepository = OrderRepository()
) {
    suspend fun loadMerchantOptions(): List<OrderMerchantOption> =
        database.suspendTransaction {
            adminOrderRepository.getMerchantOptions()
        }

    suspend fun loadOrders(filter: OrderFilter): List<OrderListItem> =
        database.suspendTransaction {
            adminOrderRepository.getOrders(filter)
        }

    suspend fun loadOrderDetailOrNull(orderId: OrderId): AdminOrderDetail? =
        database.suspendTransaction {
            adminOrderRepository.getOrderDetailOrNull(orderId)
        }

    suspend fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean =
        database.suspendTransaction {
            orderRepository.updateSubOrderStatus(subOrderId, status)
        }

    suspend fun updateOrderStatus(orderId: OrderId, status: OrderStatus): Boolean =
        database.suspendTransaction {
            orderRepository.updateOrderStatus(orderId, status)
        }
}
