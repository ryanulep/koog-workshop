package org.example.project.domain.admin

import org.example.project.db.suspendTransaction
import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.shared.ProductId
import org.jetbrains.exposed.v1.jdbc.Database

class ProductAdminService(
    private val database: Database,
    private val adminProductRepository: AdminProductRepository = AdminProductRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) {
    suspend fun loadMerchantOptions(): List<ProductMerchantOption> =
        database.suspendTransaction {
            adminProductRepository.getMerchantOptions()
        }

    suspend fun loadProducts(filter: ProductFilter): List<ProductListItem> =
        database.suspendTransaction {
            adminProductRepository.getProducts(filter)
        }

    suspend fun loadProductDetailOrNull(productId: ProductId): ProductDetail? =
        database.suspendTransaction {
            adminProductRepository.getProductDetailOrNull(productId)
        }

    suspend fun adjustStock(productId: ProductId, quantityChange: Int): Boolean =
        database.suspendTransaction {
            productRepository.updateStock(productId, quantityChange)
        }

    suspend fun setProductActive(productId: ProductId, isActive: Boolean): Boolean =
        database.suspendTransaction {
            productRepository.setProductActive(productId, isActive)
        }
}
