package org.example.project.admin.merchants

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId

class AdminMerchantService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    suspend fun loadMerchants(): List<MerchantListItem> =
        httpClient.get("$baseUrl/admin/merchants").body()

    suspend fun loadMerchantDetailOrNull(merchantId: MerchantId): MerchantDetail? =
        httpClient.get("$baseUrl/admin/merchants/${merchantId.value}").body()

    suspend fun setMerchantActive(merchantId: MerchantId, isActive: Boolean): Boolean =
        httpClient.post("$baseUrl/admin/merchants/${merchantId.value}/active") {
            parameter("isActive", isActive)
        }.body()

    suspend fun setShippingMethodActive(shippingMethodId: ShippingMethodId, isActive: Boolean): Boolean =
        httpClient.post("$baseUrl/admin/merchants/shipping-methods/${shippingMethodId.value}/active") {
            parameter("isActive", isActive)
        }.body()

    suspend fun replaceMerchantShippingMethods(
        merchantId: MerchantId,
        shippingMethodIds: Set<ShippingMethodId>
    ): Boolean = httpClient.post("$baseUrl/admin/merchants/${merchantId.value}/shipping-methods") {
        contentType(ContentType.Application.Json)
        setBody(shippingMethodIds.map { it.value }.toSet())
    }.body()
}
