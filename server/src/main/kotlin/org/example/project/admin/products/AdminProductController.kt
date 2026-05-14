package org.example.project.admin.products

import org.example.project.domain.shared.ProductId
import org.springframework.web.bind.annotation.*
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/admin/products")
class AdminProductController(
    private val adminProductService: AdminProductService
) {
    @GetMapping("/merchant-options")
    suspend fun getMerchantOptions(): List<ProductMerchantOption> =
        adminProductService.loadMerchantOptions()

    @PostMapping("/list")
    suspend fun getProducts(@RequestBody filter: ProductFilter): List<ProductListItem> =
        adminProductService.loadProducts(filter)

    @GetMapping("/{productId}")
    suspend fun getProductDetail(@PathVariable productId: Uuid): ProductDetail? =
        adminProductService.loadProductDetailOrNull(ProductId(productId))

    @PostMapping("/{productId}/stock")
    suspend fun adjustStock(
        @PathVariable productId: Uuid,
        @RequestParam quantityChange: Int
    ): Boolean =
        adminProductService.adjustStock(ProductId(productId), quantityChange)

    @PostMapping("/{productId}/active")
    suspend fun setProductActive(
        @PathVariable productId: Uuid,
        @RequestParam isActive: Boolean
    ): Boolean =
        adminProductService.setProductActive(ProductId(productId), isActive)
}
