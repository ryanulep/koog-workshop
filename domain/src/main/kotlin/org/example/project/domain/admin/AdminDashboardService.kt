package org.example.project.domain.admin

import org.example.project.domain.character.CharacterRepository
import org.example.project.domain.currency.CurrencyRepository
import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.order.OrderRepository
import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.shipping.ShippingRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.shared.OrderId
import org.jetbrains.exposed.v1.jdbc.Database

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

            val history = buildOrderHistoryEvents(
                orderId = order.id,
                orderStatus = order.status.name,
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

}
