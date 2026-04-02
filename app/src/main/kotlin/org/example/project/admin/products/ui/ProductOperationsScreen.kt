@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin.products.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.example.project.admin.products.ProductAdminUiState
import org.example.project.admin.shared.ui.AdminAccessibility
import org.example.project.admin.shared.ui.AdminChromeSectionPadding
import org.example.project.admin.shared.ui.AdminMetric
import org.example.project.admin.shared.ui.AdminMetricsRow
import org.example.project.admin.shared.ui.AdminScreenPadding
import org.example.project.admin.shared.ui.AdminSectionSpacing
import org.example.project.admin.shared.ui.DetailBlock
import org.example.project.admin.shared.ui.FilterGroup
import org.example.project.admin.shared.ui.InlineErrorCard
import org.example.project.admin.shared.ui.PanelEmptyState
import org.example.project.admin.shared.ui.PanelHeader
import org.example.project.admin.shared.ui.ToolbarTextFilter
import org.example.project.admin.shared.ui.formatAdminInstant
import org.example.project.admin.shared.ui.formatAmount
import org.example.project.admin.shared.ui.labelize
import org.example.project.admin.shared.ui.toDisplayText
import org.example.project.domain.admin.ProductActiveFilter
import org.example.project.domain.admin.ProductDetail
import org.example.project.domain.admin.ProductListItem
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ProductId

internal fun productActiveFilterCount(uiState: ProductAdminUiState): Int =
    productSecondaryFilterSummaries(uiState).size +
        (if (uiState.filter.nameQuery.isNotBlank()) 1 else 0) +
        (if (uiState.filter.activeFilter != _root_ide_package_.org.example.project.domain.admin.ProductActiveFilter.ALL) 1 else 0)

@Composable
internal fun ProductFilterContent(
    uiState: ProductAdminUiState,
    onUpdateNameQuery: (String) -> Unit,
    onUpdateActiveFilter: (org.example.project.domain.admin.ProductActiveFilter) -> Unit,
    onUpdateCategory: (org.example.project.domain.catalog.ProductCategory?) -> Unit,
    onUpdateMerchant: (org.example.project.domain.shared.MerchantId?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)) {
        ToolbarTextFilter(
            value = uiState.filter.nameQuery,
            onValueChange = onUpdateNameQuery,
            label = "Product name",
            placeholder = "Search products",
            accessibilityDescription = AdminAccessibility.ProductNameFilter
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            _root_ide_package_.org.example.project.domain.admin.ProductActiveFilter.entries.forEach { filter ->
                FilterChip(
                    modifier = Modifier.semantics {
                        contentDescription = AdminAccessibility.productActiveFilter(filter)
                    },
                    selected = filter == uiState.filter.activeFilter,
                    onClick = { onUpdateActiveFilter(filter) },
                    label = { Text(filter.labelize()) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FilterGroup(
                title = "Category",
                options = persistentListOf<Pair<String, org.example.project.domain.catalog.ProductCategory?>>("All" to null)
                    .addAll(_root_ide_package_.org.example.project.domain.catalog.ProductCategory.entries.map { it.labelize() to it }),
                selected = uiState.filter.category,
                onSelect = onUpdateCategory,
                optionContentDescription = { _, value -> AdminAccessibility.productCategoryFilter(value) },
                modifier = Modifier.weight(1f)
            )
            FilterGroup(
                title = "Merchant",
                options = persistentListOf<Pair<String, org.example.project.domain.shared.MerchantId?>>("All" to null)
                    .addAll(uiState.merchants.map { it.name to it.id }),
                selected = uiState.filter.merchantId,
                onSelect = onUpdateMerchant,
                optionContentDescription = { label, _ -> AdminAccessibility.productMerchantFilter(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ProductOperationsScreen(
    uiState: ProductAdminUiState,
    onSelectProduct: (org.example.project.domain.shared.ProductId) -> Unit,
    onAdjustStock: (Int) -> Unit,
    onSetActive: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AdminScreenPadding, vertical = AdminChromeSectionPadding),
        verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
    ) {
        uiState.errorMessage?.let { message ->
            InlineErrorCard(message = message)
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
        ) {
            ProductListPanel(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                products = uiState.products,
                selectedProductId = uiState.selectedProductId,
                onSelectProduct = onSelectProduct
            )

            ProductDetailPanel(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                product = uiState.selectedProduct,
                onAdjustStock = onAdjustStock,
                onSetActive = onSetActive
            )
        }
    }
}

@Composable
private fun ProductListPanel(
    modifier: Modifier,
    products: PersistentList<org.example.project.domain.admin.ProductListItem>,
    selectedProductId: org.example.project.domain.shared.ProductId?,
    onSelectProduct: (org.example.project.domain.shared.ProductId) -> Unit
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = AdminAccessibility.ProductListPanel
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AdminScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
        ) {
            PanelHeader(
                title = "Catalog",
                subtitle = "${products.size} matching products"
            )

            if (products.isEmpty()) {
                PanelEmptyState(message = "No products match the current filters.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
                ) {
                    items(
                        items = products,
                        key = { product -> product.id.value }
                    ) { product ->
                        ProductRow(
                            product = product,
                            selected = product.id == selectedProductId,
                            onClick = { onSelectProduct(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: org.example.project.domain.admin.ProductListItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = product.productRowAccessibilityDescription()
            stateDescription = product.productRowAccessibilityState()
        },
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${product.category.labelize()} · ${product.merchantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onClick,
                    label = { Text(if (product.isActive) "Active" else "Inactive") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.price.formatAmount(product.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Stock ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = product.reviewSummary.toDisplayText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductDetailPanel(
    modifier: Modifier,
    product: org.example.project.domain.admin.ProductDetail?,
    onAdjustStock: (Int) -> Unit,
    onSetActive: (Boolean) -> Unit
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = AdminAccessibility.ProductDetailPanel
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        when (product) {
            null -> PanelEmptyState(
                modifier = Modifier.fillMaxSize(),
                message = "Select a product to inspect its details and operations."
            )

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AdminScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PanelHeader(
                        title = product.name,
                        subtitle = product.category.labelize()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.id.value.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            product.description?.takeIf(String::isNotBlank)?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AssistChip(
                            onClick = {},
                            label = { Text(if (product.isActive) "Active" else "Inactive") }
                        )
                    }

                    AdminMetricsRow(
                        persistentListOf(
                            AdminMetric("Price", product.price.formatAmount(product.currencyCode)),
                            AdminMetric("Stock", product.stock.toString()),
                            AdminMetric("Rarity", product.rarity.labelize())
                        )
                    )
                    AdminMetricsRow(
                        persistentListOf(
                            AdminMetric("Merchant", product.merchantName),
                            AdminMetric("Currency", "${product.currencyCode} (${product.currencySymbol})"),
                            AdminMetric("Reviews", product.reviewSummary.toDisplayText())
                        )
                    )
                    AdminMetricsRow(
                        persistentListOf(
                            AdminMetric("Created", product.createdAt.formatAdminInstant()),
                            AdminMetric("Updated", product.updatedAt.formatAdminInstant())
                        )
                    )

                    product.imageUrl?.let { imageUrl ->
                        DetailBlock(
                            title = "Image URL",
                            body = imageUrl
                        )
                    }

                    if (product.categoryAttributes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Category fields",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            product.categoryAttributes.forEach { attribute ->
                                DetailBlock(
                                    title = attribute.label,
                                    body = attribute.value
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(-5, -1, 1, 5).forEach { quantityChange ->
                                FilledTonalButton(
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            stockAdjustmentAccessibilityDescription(quantityChange)
                                    },
                                    onClick = { onAdjustStock(quantityChange) }
                                ) {
                                    Text(
                                        text = if (quantityChange > 0) {
                                            "+$quantityChange stock"
                                        } else {
                                            "$quantityChange stock"
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            modifier = Modifier.semantics {
                                contentDescription =
                                    productActivationAccessibilityDescription(product.isActive)
                            },
                            onClick = { onSetActive(!product.isActive) }
                        ) {
                            Text(if (product.isActive) "Deactivate product" else "Activate product")
                        }
                    }
                }
            }
        }
    }
}

private fun productSecondaryFilterSummaries(uiState: ProductAdminUiState): List<String> =
    listOfNotNull(
        uiState.filter.category?.let { category -> "Category: ${category.labelize()}" },
        uiState.filter.merchantId?.let { merchantId ->
            val merchantName = uiState.merchants.firstOrNull { merchant -> merchant.id == merchantId }?.name
                ?: "Selected merchant"
            "Merchant: $merchantName"
        }
    )

private fun org.example.project.domain.admin.ProductListItem.productRowAccessibilityDescription(): String =
    "Product $name from $merchantName"

private fun org.example.project.domain.admin.ProductListItem.productRowAccessibilityState(): String =
    "${if (isActive) "Active" else "Inactive"}, stock $stock"

private fun stockAdjustmentAccessibilityDescription(quantityChange: Int): String =
    if (quantityChange >= 0) {
        "Increase stock by $quantityChange"
    } else {
        "Decrease stock by ${-quantityChange}"
    }

private fun productActivationAccessibilityDescription(isActive: Boolean): String =
    if (isActive) {
        "Deactivate product"
    } else {
        "Activate product"
    }
