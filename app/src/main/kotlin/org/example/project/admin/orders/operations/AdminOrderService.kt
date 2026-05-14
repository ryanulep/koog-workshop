package org.example.project.admin.orders.operations

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId

class AdminOrderService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    suspend fun loadMerchantOptions(): List<OrderMerchantOption> =
        httpClient.get("$baseUrl/admin/orders/merchants").body()

    suspend fun loadOrders(filter: OrderFilter): List<OrderListItem> =
        httpClient.post("$baseUrl/admin/orders/list") {
            contentType(ContentType.Application.Json)
            setBody(filter)
        }.body()

    suspend fun loadOrderDetailOrNull(orderId: OrderId): AdminOrderDetail? =
        httpClient.get("$baseUrl/admin/orders/${orderId.value}").body()

    suspend fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean =
        httpClient.patch("$baseUrl/admin/orders/sub-orders/${subOrderId.value}/status") {
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status))
        }.body()

    suspend fun updateOrderStatus(orderId: OrderId, status: OrderStatus): Boolean =
        httpClient.patch("$baseUrl/admin/orders/${orderId.value}/status") {
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status))
        }.body()
}
