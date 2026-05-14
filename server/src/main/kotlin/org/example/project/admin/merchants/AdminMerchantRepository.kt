package org.example.project.admin.merchants

import kotlin.time.Duration.Companion.days
import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.catalog.Products
import org.example.project.domain.currency.CurrencyRepository
import org.example.project.domain.order.SubOrders
import org.example.project.domain.shipping.ShippingRepository
import org.example.project.domain.shared.MerchantId
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class AdminMerchantRepository(
    private val merchantRepository: MerchantRepository,
    private val shippingRepository: ShippingRepository,
    private val currencyRepository: CurrencyRepository,
) {

    
    fun getMerchants(): List<MerchantListItem> {
        val productCountByMerchantId = Products.selectAll()
            .groupingBy { row -> MerchantId(row[Products.merchant].value) }
            .eachCount()
        val recentOrderCountByMerchantId = getRecentOrderCountByMerchantId()

        return merchantRepository.getAllMerchants()
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id.value.toString() }))
            .map { merchant ->
                MerchantListItem(
                    id = merchant.id,
                    name = merchant.name,
                    location = merchant.location,
                    isActive = merchant.isActive,
                    productCount = productCountByMerchantId[merchant.id] ?: 0,
                    recentOrderCount = recentOrderCountByMerchantId[merchant.id] ?: 0
                )
            }
    }

    
    fun getMerchantDetailOrNull(merchantId: MerchantId): MerchantDetail? {
        val merchant = merchantRepository.getMerchantOrNull(merchantId) ?: return null
        val currenciesById = currencyRepository.getAllCurrencies().associateBy { currency -> currency.id }
        val assignedShippingMethodIds = shippingRepository.getShippingMethodsForMerchant(merchantId)
            .mapTo(linkedSetOf()) { shippingMethod -> shippingMethod.id }
        val availableShippingMethods = shippingRepository.getAllShippingMethods()
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id.value.toString() }))
            .map { shippingMethod ->
                ShippingMethodAssignmentItem(
                    id = shippingMethod.id,
                    name = shippingMethod.name,
                    description = shippingMethod.description,
                    baseCost = shippingMethod.baseCost,
                    currencyCode = currenciesById[shippingMethod.currencyId]?.code ?: "UNK",
                    estimatedDays = shippingMethod.estimatedDays,
                    isActive = shippingMethod.isActive,
                    isAssigned = shippingMethod.id in assignedShippingMethodIds,
                    createdAt = shippingMethod.createdAt,
                    updatedAt = shippingMethod.updatedAt
                )
            }
        val productCount = Products.selectAll()
            .where { Products.merchant eq merchantId.value }
            .count().toInt()
        val recentOrderCount = getRecentOrderCountByMerchantId()[merchantId] ?: 0

        return MerchantDetail(
            merchant = merchant,
            productCount = productCount,
            recentOrderCount = recentOrderCount,
            assignedShippingMethods = availableShippingMethods.filter { shippingMethod -> shippingMethod.isAssigned },
            availableShippingMethods = availableShippingMethods
        )
    }

    
    private fun getRecentOrderCountByMerchantId(): Map<MerchantId, Int> {
        val subOrderRows = SubOrders.selectAll().toList()
        if (subOrderRows.isEmpty()) return emptyMap()

        val recentThreshold = subOrderRows.maxOf { row -> row[SubOrders.createdAt] } - 30.days

        return subOrderRows
            .asSequence()
            .filter { row -> row[SubOrders.createdAt] >= recentThreshold }
            .groupBy { row -> MerchantId(row[SubOrders.merchant].value) }
            .mapValues { (_, rows) ->
                rows.map { row -> row[SubOrders.order].value }
                    .distinct()
                    .size
            }
    }
}
