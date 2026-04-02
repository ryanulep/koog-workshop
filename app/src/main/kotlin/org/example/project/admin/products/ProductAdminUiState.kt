package org.example.project.admin.products

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.example.project.domain.admin.ProductDetail
import org.example.project.domain.admin.ProductFilter
import org.example.project.domain.admin.ProductListItem
import org.example.project.domain.admin.ProductMerchantOption
import org.example.project.domain.shared.ProductId

@Immutable
data class ProductAdminUiState(
    val errorMessage: String? = null,
    val filter: org.example.project.domain.admin.ProductFilter = _root_ide_package_.org.example.project.domain.admin.ProductFilter(),
    val merchants: PersistentList<org.example.project.domain.admin.ProductMerchantOption> = persistentListOf(),
    val products: PersistentList<org.example.project.domain.admin.ProductListItem> = persistentListOf(),
    val selectedProductId: org.example.project.domain.shared.ProductId? = null,
    val selectedProduct: org.example.project.domain.admin.ProductDetail? = null
)
