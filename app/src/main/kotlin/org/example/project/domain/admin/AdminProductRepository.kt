package org.example.project.domain.admin

import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.catalog.Product
import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.currency.CurrencyRepository
import org.example.project.domain.review.Reviews
import org.example.project.domain.shared.ProductId
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.selectAll

class AdminProductRepository(
    private val productRepository: ProductRepository = ProductRepository(),
    private val merchantRepository: MerchantRepository = MerchantRepository(),
    private val currencyRepository: CurrencyRepository = CurrencyRepository()
) {

    context(_: Transaction)
    fun getMerchantOptions(): List<ProductMerchantOption> =
        merchantRepository.getAllMerchants()
            .sortedBy { it.name.lowercase() }
            .map { merchant ->
                ProductMerchantOption(
                    id = merchant.id,
                    name = merchant.name
                )
            }

    context(_: Transaction)
    fun getProducts(filter: ProductFilter): List<ProductListItem> {
        val merchantsById = merchantRepository.getAllMerchants().associateBy { it.id }
        val currenciesById = currencyRepository.getAllCurrencies().associateBy { it.id }
        val reviewSummaryByProductId = getReviewSummaryByProductId()

        return productRepository.getAllProducts()
            .asSequence()
            .filter { product -> product.matches(filter) }
            .map { product ->
                ProductListItem(
                    id = product.id,
                    name = product.name,
                    category = product.category,
                    merchantName = merchantsById[product.merchantId]?.name ?: "Unknown merchant",
                    price = product.price,
                    currencyCode = currenciesById[product.currencyId]?.code ?: "UNK",
                    stock = product.stock,
                    isActive = product.isActive,
                    reviewSummary = reviewSummaryByProductId[product.id] ?: ProductReviewSummary()
                )
            }
            .sortedWith(compareBy<ProductListItem> { it.name.lowercase() }.thenBy { it.id.value.toString() })
            .toList()
    }

    context(_: Transaction)
    fun getProductDetailOrNull(productId: ProductId): ProductDetail? {
        val product = productRepository.getProductOrNull(productId) ?: return null
        val merchant = merchantRepository.getMerchantOrNull(product.merchantId)
        val currency = currencyRepository.getCurrencyOrNull(product.currencyId)
        val reviewSummary = getReviewSummaryByProductId()[product.id] ?: ProductReviewSummary()

        return ProductDetail(
            id = product.id,
            name = product.name,
            description = product.description,
            category = product.category,
            rarity = product.rarity,
            price = product.price,
            currencyCode = currency?.code ?: "UNK",
            currencySymbol = currency?.symbol ?: "?",
            merchantId = product.merchantId,
            merchantName = merchant?.name ?: "Unknown merchant",
            stock = product.stock,
            isActive = product.isActive,
            imageUrl = product.imageUrl,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
            reviewSummary = reviewSummary,
            categoryAttributes = product.toCategoryAttributes()
        )
    }

    context(_: Transaction)
    private fun getReviewSummaryByProductId(): Map<ProductId, ProductReviewSummary> =
        Reviews.selectAll()
            .groupBy { row -> ProductId(row[Reviews.product].value) }
            .mapValues { (_, rows) ->
                val ratings = rows.map { row -> row[Reviews.rating].toDouble() }
                ProductReviewSummary(
                    averageRating = ratings.takeIf { it.isNotEmpty() }?.average(),
                    reviewCount = ratings.size
                )
            }

    private fun Product.matches(filter: ProductFilter): Boolean {
        if (filter.category != null && category != filter.category) return false
        if (filter.merchantId != null && merchantId != filter.merchantId) return false
        if (!matchesActiveFilter(filter.activeFilter)) return false

        val normalizedQuery = filter.nameQuery.trim().lowercase()
        if (normalizedQuery.isBlank()) return true

        return name.lowercase().contains(normalizedQuery)
    }

    private fun Product.matchesActiveFilter(activeFilter: ProductActiveFilter): Boolean =
        when (activeFilter) {
            ProductActiveFilter.ALL -> true
            ProductActiveFilter.ACTIVE -> isActive
            ProductActiveFilter.INACTIVE -> !isActive
        }

    private fun Product.toCategoryAttributes(): List<ProductDetailAttribute> =
        when (this) {
            is Product.Weapon -> listOf(
                ProductDetailAttribute("Damage", damage.toString()),
                ProductDetailAttribute("Damage type", damageType.name),
                ProductDetailAttribute("Weapon slot", weaponSlot.name)
            )

            is Product.Armor -> listOf(
                ProductDetailAttribute("Defense", defense.toString()),
                ProductDetailAttribute("Armor slot", armorSlot.name)
            )

            is Product.Potion -> buildList {
                add(ProductDetailAttribute("Effect", effect))
                duration?.let { turns ->
                    add(ProductDetailAttribute("Duration", "$turns turns"))
                }
            }

            is Product.Scroll -> listOf(
                ProductDetailAttribute("Spell", spellName),
                ProductDetailAttribute("Spell level", spellLevel.toString())
            )

            is Product.MiscItem -> emptyList()
        }
}
