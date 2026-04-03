@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin.merchants.ui

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
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import org.example.project.admin.merchants.MerchantAdminUiState
import org.example.project.admin.shared.ui.AdminAccessibility
import org.example.project.admin.shared.ui.AdminChromeSectionPadding
import org.example.project.admin.shared.ui.AdminMetric
import org.example.project.admin.shared.ui.AdminMetricsRow
import org.example.project.admin.shared.ui.AdminScreenPadding
import org.example.project.admin.shared.ui.AdminSectionSpacing
import org.example.project.admin.shared.ui.DetailBlock
import org.example.project.admin.shared.ui.InlineErrorCard
import org.example.project.admin.shared.ui.PanelEmptyState
import org.example.project.admin.shared.ui.PanelHeader
import org.example.project.admin.shared.ui.formatAdminInstant
import org.example.project.admin.shared.ui.formatAmount
import org.example.project.domain.admin.merchants.MerchantDetail
import org.example.project.domain.admin.merchants.MerchantListItem
import org.example.project.domain.admin.merchants.ShippingMethodAssignmentItem
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId

@Composable
fun MerchantOperationsScreen(
    uiState: MerchantAdminUiState,
    onSelectMerchant: (MerchantId) -> Unit,
    onSetMerchantActive: (Boolean) -> Unit,
    onSetShippingMethodActive: (ShippingMethodId, Boolean) -> Unit,
    onUpdateShippingAssignmentSelection: (ShippingMethodId, Boolean) -> Unit,
    onSaveShippingAssignments: () -> Unit
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
            MerchantListPanel(
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxHeight(),
                merchants = uiState.merchants,
                selectedMerchantId = uiState.selectedMerchantId,
                onSelectMerchant = onSelectMerchant
            )

            MerchantDetailPanel(
                modifier = Modifier
                    .weight(1.18f)
                    .fillMaxHeight(),
                merchantDetail = uiState.selectedMerchant,
                selectedShippingMethodIds = uiState.selectedShippingMethodIds,
                hasPendingShippingAssignments = uiState.hasPendingShippingAssignments,
                onSetMerchantActive = onSetMerchantActive,
                onSetShippingMethodActive = onSetShippingMethodActive,
                onUpdateShippingAssignmentSelection = onUpdateShippingAssignmentSelection,
                onSaveShippingAssignments = onSaveShippingAssignments
            )
        }
    }
}

@Composable
private fun MerchantListPanel(
    modifier: Modifier,
    merchants: PersistentList<MerchantListItem>,
    selectedMerchantId: MerchantId?,
    onSelectMerchant: (MerchantId) -> Unit
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = AdminAccessibility.MerchantListPanel
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
                title = "Merchants",
                subtitle = "${merchants.size} merchants"
            )

            if (merchants.isEmpty()) {
                PanelEmptyState(message = "No merchants are available.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AdminSectionSpacing)
                ) {
                    items(
                        items = merchants,
                        key = { merchant -> merchant.id.value }
                    ) { merchant ->
                        MerchantRow(
                            merchant = merchant,
                            selected = merchant.id == selectedMerchantId,
                            onClick = { onSelectMerchant(merchant.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantRow(
    merchant: MerchantListItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = merchant.merchantRowAccessibilityDescription()
            stateDescription = merchant.merchantRowAccessibilityState()
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
                        text = merchant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = merchant.location ?: "Location unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onClick,
                    label = { Text(if (merchant.isActive) "Active" else "Inactive") }
                )
            }

            AdminMetricsRow(
                persistentListOf(
                    AdminMetric("Products", merchant.productCount.toString()),
                    AdminMetric("Recent orders", merchant.recentOrderCount.toString())
                )
            )
        }
    }
}

@Composable
private fun MerchantDetailPanel(
    modifier: Modifier,
    merchantDetail: MerchantDetail?,
    selectedShippingMethodIds: PersistentSet<ShippingMethodId>,
    hasPendingShippingAssignments: Boolean,
    onSetMerchantActive: (Boolean) -> Unit,
    onSetShippingMethodActive: (ShippingMethodId, Boolean) -> Unit,
    onUpdateShippingAssignmentSelection: (ShippingMethodId, Boolean) -> Unit,
    onSaveShippingAssignments: () -> Unit
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = AdminAccessibility.MerchantDetailPanel
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        when (merchantDetail) {
            null -> PanelEmptyState(
                modifier = Modifier.fillMaxSize(),
                message = "Select a merchant to inspect metadata and manage shipping assignments."
            )

            else -> {
                val merchant = merchantDetail.merchant
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AdminScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PanelHeader(
                        title = merchant.name,
                        subtitle = merchant.location ?: "Location unavailable"
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
                                text = merchant.id.value.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            merchant.description?.takeIf(String::isNotBlank)?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AssistChip(
                            onClick = {},
                            label = { Text(if (merchant.isActive) "Active" else "Inactive") }
                        )
                    }

                    AdminMetricsRow(
                        persistentListOf(
                            AdminMetric("Products", merchantDetail.productCount.toString()),
                            AdminMetric("Recent orders", merchantDetail.recentOrderCount.toString()),
                            AdminMetric("Assigned shipping", merchantDetail.assignedShippingMethods.size.toString())
                        )
                    )
                    AdminMetricsRow(
                        persistentListOf(
                            AdminMetric("Theme", merchant.theme ?: "Not set"),
                            AdminMetric("Created", merchant.createdAt.formatAdminInstant()),
                            AdminMetric("Updated", merchant.updatedAt.formatAdminInstant())
                        )
                    )

                    merchant.iconPath?.takeIf(String::isNotBlank)?.let { iconPath ->
                        DetailBlock(
                            title = "Icon path",
                            body = iconPath
                        )
                    }

                    AssignedShippingMethodsSection(
                        assignedShippingMethods = merchantDetail.assignedShippingMethods
                    )

                    ShippingAssignmentsEditor(
                        merchantName = merchant.name,
                        availableShippingMethods = merchantDetail.availableShippingMethods,
                        selectedShippingMethodIds = selectedShippingMethodIds,
                        hasPendingShippingAssignments = hasPendingShippingAssignments,
                        onSetShippingMethodActive = onSetShippingMethodActive,
                        onUpdateShippingAssignmentSelection = onUpdateShippingAssignmentSelection,
                        onSaveShippingAssignments = onSaveShippingAssignments
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedButton(
                            modifier = Modifier.semantics {
                                contentDescription =
                                    merchantActivationAccessibilityDescription(merchant.isActive)
                            },
                            onClick = { onSetMerchantActive(!merchant.isActive) }
                        ) {
                            Text(if (merchant.isActive) "Deactivate merchant" else "Activate merchant")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignedShippingMethodsSection(
    assignedShippingMethods: List<ShippingMethodAssignmentItem>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Assigned shipping methods",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (assignedShippingMethods.isEmpty()) {
            DetailBlock(
                title = "Assignments",
                body = "No shipping methods are currently assigned to this merchant."
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                assignedShippingMethods.forEach { shippingMethod ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${shippingMethod.name} · " +
                                    shippingMethod.baseCost.formatAmount(shippingMethod.currencyCode)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShippingAssignmentsEditor(
    merchantName: String,
    availableShippingMethods: List<ShippingMethodAssignmentItem>,
    selectedShippingMethodIds: PersistentSet<ShippingMethodId>,
    hasPendingShippingAssignments: Boolean,
    onSetShippingMethodActive: (ShippingMethodId, Boolean) -> Unit,
    onUpdateShippingAssignmentSelection: (ShippingMethodId, Boolean) -> Unit,
    onSaveShippingAssignments: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Shipping assignments",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (availableShippingMethods.isEmpty()) {
            DetailBlock(
                title = "Shipping methods",
                body = "No shipping methods are available yet."
            )
        } else {
            availableShippingMethods.forEach { shippingMethod ->
                val isAssigned = shippingMethod.id in selectedShippingMethodIds
                ShippingMethodEditorRow(
                    shippingMethod = shippingMethod,
                    isAssigned = isAssigned,
                    onSetActive = { isActive ->
                        onSetShippingMethodActive(shippingMethod.id, isActive)
                    },
                    onSetAssigned = { selected ->
                        onUpdateShippingAssignmentSelection(shippingMethod.id, selected)
                    }
                )
            }
        }

        OutlinedButton(
            modifier = Modifier.semantics {
                contentDescription = saveShippingAssignmentsAccessibilityDescription(merchantName)
            },
            enabled = hasPendingShippingAssignments,
            onClick = onSaveShippingAssignments
        ) {
            Text("Save shipping assignments")
        }
    }
}

@Composable
private fun ShippingMethodEditorRow(
    shippingMethod: ShippingMethodAssignmentItem,
    isAssigned: Boolean,
    onSetActive: (Boolean) -> Unit,
    onSetAssigned: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = shippingMethod.shippingMethodAccessibilityDescription()
            stateDescription = shippingMethod.shippingMethodAccessibilityState(isAssigned = isAssigned)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                        text = shippingMethod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    shippingMethod.description?.takeIf(String::isNotBlank)?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssistChip(
                    onClick = {},
                    label = { Text(if (shippingMethod.isActive) "Active" else "Inactive") }
                )
            }

            AdminMetricsRow(
                persistentListOf(
                    AdminMetric("Base cost", shippingMethod.baseCost.formatAmount(shippingMethod.currencyCode)),
                    AdminMetric("ETA", "${shippingMethod.estimatedDays} days"),
                    AdminMetric("Updated", shippingMethod.updatedAt.formatAdminInstant())
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    modifier = Modifier.semantics {
                        contentDescription =
                            shippingAssignmentAccessibilityDescription(shippingMethod.name, isAssigned)
                    },
                    selected = isAssigned,
                    onClick = { onSetAssigned(!isAssigned) },
                    label = { Text(if (isAssigned) "Assigned" else "Available") }
                )

                OutlinedButton(
                    modifier = Modifier.semantics {
                        contentDescription =
                            shippingMethodActivationAccessibilityDescription(
                                shippingMethod.name,
                                shippingMethod.isActive
                            )
                    },
                    onClick = { onSetActive(!shippingMethod.isActive) }
                ) {
                    Text(if (shippingMethod.isActive) "Deactivate" else "Activate")
                }
            }
        }
    }
}

private fun MerchantListItem.merchantRowAccessibilityDescription(): String =
    "Merchant $name"

private fun MerchantListItem.merchantRowAccessibilityState(): String =
    "${if (isActive) "Active" else "Inactive"}, " +
        "${productCount.productLabel()}, ${recentOrderCount.recentOrderLabel()}"

private fun ShippingMethodAssignmentItem.shippingMethodAccessibilityDescription(): String =
    "Shipping method $name"

private fun ShippingMethodAssignmentItem.shippingMethodAccessibilityState(isAssigned: Boolean): String =
    "${if (isAssigned) "Assigned" else "Available"}, ${if (isActive) "Active" else "Inactive"}"

private fun Int.productLabel(): String =
    if (this == 1) "1 product" else "$this products"

private fun Int.recentOrderLabel(): String =
    if (this == 1) "1 recent order" else "$this recent orders"

private fun merchantActivationAccessibilityDescription(isActive: Boolean): String =
    if (isActive) {
        "Deactivate merchant"
    } else {
        "Activate merchant"
    }

private fun shippingMethodActivationAccessibilityDescription(
    shippingMethodName: String,
    isActive: Boolean
): String = if (isActive) {
    "Deactivate shipping method $shippingMethodName"
} else {
    "Activate shipping method $shippingMethodName"
}

private fun shippingAssignmentAccessibilityDescription(
    shippingMethodName: String,
    isAssigned: Boolean
): String = if (isAssigned) {
    "Remove shipping method $shippingMethodName from merchant assignments"
} else {
    "Assign shipping method $shippingMethodName to merchant"
}

private fun saveShippingAssignmentsAccessibilityDescription(merchantName: String): String =
    "Save shipping assignments for $merchantName"
