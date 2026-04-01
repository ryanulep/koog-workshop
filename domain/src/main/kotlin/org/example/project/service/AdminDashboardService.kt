package org.example.project.service

import org.example.project.db.repository.AdminDashboardRepository
import org.example.project.db.repository.CharacterRepository
import org.example.project.db.repository.CurrencyRepository
import org.example.project.db.repository.MerchantRepository
import org.example.project.db.repository.OrderRepository
import org.example.project.db.repository.ProductRepository
import org.example.project.db.repository.ShippingRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.id.OrderId
import org.example.project.domain.model.AdminOrderDetail
import org.example.project.domain.model.AdminOrderHistoryEvent
import org.example.project.domain.model.AdminOrderItemDetail
import org.example.project.domain.model.AdminSubOrderDetail
import org.example.project.domain.model.RecentOrderSummary
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.math.abs

class AdminDashboardService(
    private val database: Database,
    private val dashboardRepository: AdminDashboardRepository = AdminDashboardRepository(),
    private val orderRepository: OrderRepository = OrderRepository(),
    private val characterRepository: CharacterRepository = CharacterRepository(),
    private val currencyRepository: CurrencyRepository = CurrencyRepository(),
    private val merchantRepository: MerchantRepository = MerchantRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
    private val shippingRepository: ShippingRepository = ShippingRepository()
) {
    suspend fun loadRecentOrders(limit: Int = 5): List<RecentOrderSummary> =
        database.suspendTransaction {
            dashboardRepository.getRecentOrders(limit)
        }

    suspend fun loadOrderHistory(): List<RecentOrderSummary> =
        database.suspendTransaction {
            dashboardRepository.getOrderSummaries(limit = null)
        }

    suspend fun loadOrderDetailsOrNull(orderId: OrderId): AdminOrderDetail? =
        database.suspendTransaction {
            val order = orderRepository.getOrderOrNull(orderId) ?: return@suspendTransaction null
            val characterName = characterRepository.getCharacterOrNull(order.characterId)?.name
                ?: "Unknown character"
            val currencyCode = currencyRepository.getCurrencyOrNull(order.totalCurrencyId)?.code
                ?: "Unknown currency"
            val transactions = orderRepository.getOrderTransactions(orderId)

            val subOrders = orderRepository.getSubOrders(orderId).map { subOrder ->
                val merchantName = merchantRepository.getMerchantOrNull(subOrder.merchantId)?.name
                    ?: "Unknown merchant"
                val shippingMethod = shippingRepository.getShippingMethodByIdOrNull(subOrder.shippingMethodId)
                val shippingCurrencyCode = shippingMethod?.let {
                    currencyRepository.getCurrencyOrNull(it.currencyId)?.code
                } ?: currencyCode
                val items = orderRepository.getOrderItems(subOrder.id).map { item ->
                    val product = productRepository.getProductOrNull(item.productId)
                    val itemCurrencyCode = currencyRepository.getCurrencyOrNull(item.snapshottedCurrencyId)?.code
                        ?: currencyCode
                    AdminOrderItemDetail(
                        item = item,
                        productName = product?.name ?: "Unknown product",
                        productCategory = product?.category?.name ?: "Unknown category",
                        productDescription = product?.description,
                        merchantName = merchantName,
                        currencyCode = itemCurrencyCode,
                        unitPrice = item.snapshottedPrice,
                        subtotal = item.snapshottedPrice * item.quantity.toLong()
                    )
                }

                AdminSubOrderDetail(
                    subOrder = subOrder,
                    merchantName = merchantName,
                    shippingMethodName = shippingMethod?.name ?: "Unknown shipping method",
                    shippingCostCurrencyCode = shippingCurrencyCode,
                    items = items
                )
            }

            val history = buildHistoryEvents(
                orderId = order.id,
                orderStatus = order.status,
                orderCreatedAt = order.createdAt,
                orderUpdatedAt = order.updatedAt,
                currencyCode = currencyCode,
                subOrders = subOrders,
                transactions = transactions
            )

            AdminOrderDetail(
                order = order,
                characterName = characterName,
                currencyCode = currencyCode,
                subOrders = subOrders,
                history = history
            )
        }

    private fun buildHistoryEvents(
        orderId: OrderId,
        orderStatus: OrderStatus,
        orderCreatedAt: kotlin.time.Instant,
        orderUpdatedAt: kotlin.time.Instant,
        currencyCode: String,
        subOrders: List<AdminSubOrderDetail>,
        transactions: List<org.example.project.domain.model.Transaction>
    ): List<AdminOrderHistoryEvent> {
        data class TimelineEntry(
            val timestamp: kotlin.time.Instant,
            val priority: Int,
            val title: String,
            val description: String
        )

        val entries = buildList {
            add(
                TimelineEntry(
                    timestamp = orderCreatedAt,
                    priority = 0,
                    title = "Order created",
                    description = "Order ${orderId.value} was created."
                )
            )

            if (orderUpdatedAt != orderCreatedAt) {
                add(
                    TimelineEntry(
                        timestamp = orderUpdatedAt,
                        priority = 4,
                        title = "Order updated",
                        description = "Order ${orderId.value} is now $orderStatus."
                    )
                )
            }

            subOrders.forEach { subOrderDetail ->
                val subOrder = subOrderDetail.subOrder
                add(
                    TimelineEntry(
                        timestamp = subOrder.createdAt,
                        priority = 1,
                        title = "Sub-order created",
                        description = "${subOrderDetail.merchantName} received sub-order ${subOrder.id.value}."
                    )
                )

                if (subOrder.updatedAt != subOrder.createdAt) {
                    add(
                        TimelineEntry(
                            timestamp = subOrder.updatedAt,
                            priority = 3,
                            title = "Sub-order updated",
                            description = "${subOrderDetail.merchantName} updated sub-order ${subOrder.id.value} to ${subOrder.status}."
                        )
                    )
                }
            }

            transactions.forEach { transaction ->
                val title = when (transaction.type) {
                    TransactionType.PURCHASE -> "Purchase recorded"
                    TransactionType.REFUND -> "Refund recorded"
                    TransactionType.DEPOSIT -> "Deposit recorded"
                    TransactionType.EXCHANGE_DEBIT -> "Exchange debit recorded"
                    TransactionType.EXCHANGE_CREDIT -> "Exchange credit recorded"
                }
                val description = transaction.description
                    ?: "${transaction.type.name.lowercase().replace('_', ' ')} of ${abs(transaction.amount)} $currencyCode."

                add(
                    TimelineEntry(
                        timestamp = transaction.createdAt,
                        priority = 2,
                        title = title,
                        description = description
                    )
                )
            }
        }

        return entries
            .sortedWith(compareBy<TimelineEntry> { it.timestamp }.thenBy { it.priority }.thenBy { it.title })
            .map { AdminOrderHistoryEvent(it.timestamp, it.title, it.description) }
    }
}
