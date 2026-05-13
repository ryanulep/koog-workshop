package org.example.project.domain.catalog

import org.example.project.domain.catalog.MerchantRepository
import org.example.project.domain.catalog.ProductRepository

import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.catalog.Merchant
import org.example.project.domain.shared.Page
import org.example.project.domain.catalog.Product
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class CatalogService(
    private val productRepository: ProductRepository,
    private val merchantRepository: MerchantRepository
) {
    // --- Products ---

     fun getProducts(offset: Long = 0, limit: Long = 50): Page<Product> =
        productRepository.getProducts(offset, limit)

    fun getProductOrNull(id: ProductId): Product? =
        productRepository.getProductOrNull(id)

     fun getProductsByCategory(category: ProductCategory): List<Product> =
         productRepository.getProductsByCategory(category)

    fun createProduct(product: Product): ProductId =
        productRepository.createProduct(product)

    fun updateProduct(product: Product): Boolean =
     productRepository.updateProduct(product)

    fun deleteProduct(id: ProductId): Boolean =
        productRepository.deleteProduct(id)

    // --- Merchants ---

    fun getMerchants(offset: Long = 0, limit: Long = 50): Page<Merchant> =
        merchantRepository.getMerchants(offset, limit)

    fun getMerchantOrNull(id: MerchantId): Merchant? =
        merchantRepository.getMerchantOrNull(id)

    fun createMerchant(
        name: String,
        description: String? = null,
        location: String? = null,
        theme: String? = null,
        iconPath: String? = null
    ): MerchantId =
            merchantRepository.createMerchant(name, description, location, theme, iconPath)

    fun updateMerchant(
        id: MerchantId,
        name: String? = null,
        description: String? = null,
        location: String? = null,
        theme: String? = null,
        iconPath: String? = null,
        isActive: Boolean? = null
    ): Boolean =
            merchantRepository.updateMerchant(id, name, description, location, theme, iconPath, isActive)

    fun deleteMerchant(id: MerchantId): Boolean =
        merchantRepository.deleteMerchant(id)
}
