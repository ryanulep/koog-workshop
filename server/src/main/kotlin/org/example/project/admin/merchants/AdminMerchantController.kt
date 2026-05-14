package org.example.project.admin.merchants

import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import org.springframework.web.bind.annotation.*
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/admin/merchants")
class AdminMerchantController(
    private val adminMerchantService: AdminMerchantService
) {
    @GetMapping
    suspend fun getMerchants(): List<MerchantListItem> =
        adminMerchantService.loadMerchants()

    @GetMapping("/{merchantId}")
    suspend fun getMerchantDetail(@PathVariable merchantId: Uuid): MerchantDetail? =
        adminMerchantService.loadMerchantDetailOrNull(MerchantId(merchantId))

    @PostMapping("/{merchantId}/active")
    suspend fun setMerchantActive(
        @PathVariable merchantId: Uuid,
        @RequestParam isActive: Boolean
    ): Boolean =
        adminMerchantService.setMerchantActive(MerchantId(merchantId), isActive)

    @PostMapping("/shipping-methods/{shippingMethodId}/active")
    suspend fun setShippingMethodActive(
        @PathVariable shippingMethodId: Uuid,
        @RequestParam isActive: Boolean
    ): Boolean =
        adminMerchantService.setShippingMethodActive(ShippingMethodId(shippingMethodId), isActive)

    @PostMapping("/{merchantId}/shipping-methods")
    suspend fun replaceMerchantShippingMethods(
        @PathVariable merchantId: Uuid,
        @RequestBody shippingMethodIds: Set<Uuid>
    ): Boolean =
        adminMerchantService.replaceMerchantShippingMethods(
            MerchantId(merchantId),
            shippingMethodIds.map { ShippingMethodId(it) }.toSet()
        )
}
