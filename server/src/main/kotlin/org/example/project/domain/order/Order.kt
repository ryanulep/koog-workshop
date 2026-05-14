package org.example.project.domain.order


import kotlin.time.Instant
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.*

data class Order(
    val id: OrderId,
    val characterId: CharacterId,
    val status: OrderStatus,
    val totalPrice: Long,
    val totalCurrencyId: CurrencyId,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class SubOrder(
    val id: SubOrderId,
    val orderId: OrderId,
    val merchantId: MerchantId,
    val status: OrderStatus,
    val shippingMethodId: ShippingMethodId,
    val shippingCost: Long,
    val merchantTotalPrice: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)


data class OrderItem(
    val id: OrderItemId,
    val subOrderId: SubOrderId,
    val productId: ProductId,
    val quantity: Int,
    val snapshottedPrice: Long,
    val snapshottedCurrencyId: CurrencyId,
    val createdAt: Instant,
    val updatedAt: Instant
)
