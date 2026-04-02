package org.example.project.domain.admin

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.ProductId

enum class ProductActiveFilter {
    ALL,
    ACTIVE,
    INACTIVE
}

@Immutable
data class ProductFilter(
    val nameQuery: String = "",
    val category: ProductCategory? = null,
    val merchantId: MerchantId? = null,
    val activeFilter: ProductActiveFilter = ProductActiveFilter.ALL
)

@Immutable
data class ProductMerchantOption(
    val id: MerchantId,
    val name: String
)

@Immutable
data class ProductReviewSummary(
    val averageRating: Double? = null,
    val reviewCount: Int = 0
)

@Immutable
data class ProductListItem(
    val id: ProductId,
    val name: String,
    val category: ProductCategory,
    val merchantName: String,
    val price: Long,
    val currencyCode: String,
    val stock: Int,
    val isActive: Boolean,
    val reviewSummary: ProductReviewSummary
)

@Immutable
data class ProductDetailAttribute(
    val label: String,
    val value: String
)

@Immutable
data class ProductDetail(
    val id: ProductId,
    val name: String,
    val description: String?,
    val category: ProductCategory,
    val rarity: Rarity,
    val price: Long,
    val currencyCode: String,
    val currencySymbol: String,
    val merchantId: MerchantId,
    val merchantName: String,
    val stock: Int,
    val isActive: Boolean,
    val imageUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val reviewSummary: ProductReviewSummary,
    val categoryAttributes: List<ProductDetailAttribute>
)

@Immutable
data class OrderFilter(
    val orderIdQuery: String = "",
    val orderStatus: OrderStatus? = null,
    val subOrderStatus: OrderStatus? = null,
    val merchantId: MerchantId? = null
)

@Immutable
data class OrderMerchantOption(
    val id: MerchantId,
    val name: String
)

@Immutable
data class OrderListItem(
    val orderId: OrderId,
    val characterName: String,
    val status: OrderStatus,
    val merchantCount: Int,
    val totalPrice: Long,
    val currencyCode: String,
    val createdAt: Instant
)
