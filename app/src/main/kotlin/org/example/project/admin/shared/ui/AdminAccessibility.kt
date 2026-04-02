package org.example.project.admin.shared.ui

import org.example.project.domain.admin.ProductActiveFilter
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.order.OrderStatus

internal object AdminAccessibility {
    const val ProductNameFilter = "Search products by name"
    const val ProductListPanel = "Product catalog"
    const val ProductDetailPanel = "Product details"

    const val MerchantListPanel = "Merchants list"
    const val MerchantDetailPanel = "Merchant details"

    const val OrderIdFilter = "Search orders by ID"
    const val OrderListPanel = "Orders list"
    const val OrderDetailPanel = "Order details"

    fun productActiveFilter(filter: org.example.project.domain.admin.ProductActiveFilter): String =
        when (filter) {
            _root_ide_package_.org.example.project.domain.admin.ProductActiveFilter.ALL -> "Show all products"
            _root_ide_package_.org.example.project.domain.admin.ProductActiveFilter.ACTIVE -> "Show active products"
            _root_ide_package_.org.example.project.domain.admin.ProductActiveFilter.INACTIVE -> "Show inactive products"
        }

    fun productCategoryFilter(category: org.example.project.domain.catalog.ProductCategory?): String =
        if (category == null) {
            "Filter products by category: All categories"
        } else {
            "Filter products by category: ${category.labelize()}"
        }

    fun productMerchantFilter(label: String): String =
        if (label == "All") {
            "Filter products by merchant: All merchants"
        } else {
            "Filter products by merchant: $label"
        }

    fun orderStatusFilter(status: org.example.project.domain.order.OrderStatus?): String =
        if (status == null) {
            "Filter orders by status: All statuses"
        } else {
            "Filter orders by status: ${status.labelize()}"
        }

    fun subOrderStatusFilter(status: org.example.project.domain.order.OrderStatus?): String =
        if (status == null) {
            "Filter sub-orders by status: All statuses"
        } else {
            "Filter sub-orders by status: ${status.labelize()}"
        }

    fun orderMerchantFilter(label: String): String =
        if (label == "All") {
            "Filter orders by merchant: All merchants"
        } else {
            "Filter orders by merchant: $label"
        }
}
