package org.example.project.admin.products


import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.shared.ProductId
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class AdminProductService(
    private val adminProductRepository: AdminProductRepository,
    private val productRepository: ProductRepository
) {
fun loadMerchantOptions(): List<ProductMerchantOption> =
        adminProductRepository.getMerchantOptions()

    fun loadProducts(filter: ProductFilter): List<ProductListItem> =
        adminProductRepository.getProducts(filter)

    fun loadProductDetailOrNull(productId: ProductId): ProductDetail? =
        adminProductRepository.getProductDetailOrNull(productId)

    fun adjustStock(productId: ProductId, quantityChange: Int): Boolean =
        productRepository.updateStock(productId, quantityChange)

    fun setProductActive(productId: ProductId, isActive: Boolean): Boolean =
        productRepository.setProductActive(productId, isActive)
}
