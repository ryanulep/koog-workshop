package org.example.project.admin.products

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.domain.shared.ProductId

open class AdminProductService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    open suspend fun loadMerchantOptions(): List<ProductMerchantOption> =
        httpClient.get("$baseUrl/admin/products/merchant-options").body()

    open suspend fun loadProducts(filter: ProductFilter): List<ProductListItem> =
        httpClient.post("$baseUrl/admin/products/list") {
            contentType(ContentType.Application.Json)
            setBody(filter)
        }.body()

    open suspend fun loadProductDetailOrNull(productId: ProductId): ProductDetail? =
        httpClient.get("$baseUrl/admin/products/${productId.value}").body()

    open suspend fun adjustStock(productId: ProductId, quantityChange: Int): Boolean =
        httpClient.post("$baseUrl/admin/products/${productId.value}/stock") {
            parameter("quantityChange", quantityChange)
        }.body()

    open suspend fun setProductActive(productId: ProductId, isActive: Boolean): Boolean =
        httpClient.post("$baseUrl/admin/products/${productId.value}/active") {
            parameter("isActive", isActive)
        }.body()
}
