package org.example.project.admin.products


import kotlinx.serialization.Serializable
import kotlin.time.Instant
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ProductId

@Serializable
enum class ProductActiveFilter {
    ALL,
    ACTIVE,
    INACTIVE
}


@Serializable
data class ProductFilter(
    val nameQuery: String = "",
    val category: ProductCategory? = null,
    val merchantId: MerchantId? = null,
    val activeFilter: ProductActiveFilter = ProductActiveFilter.ALL
)


@Serializable
data class ProductMerchantOption(
    val id: MerchantId,
    val name: String
)


@Serializable
data class ProductReviewSummary(
    val averageRating: Double? = null,
    val reviewCount: Int = 0
)


@Serializable
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


@Serializable
data class ProductDetailAttribute(
    val label: String,
    val value: String
)


@Serializable
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
