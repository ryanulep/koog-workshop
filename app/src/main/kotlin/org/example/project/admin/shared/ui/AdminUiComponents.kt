package org.example.project.admin.shared.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

internal data class AdminMetric(
    val label: String,
    val value: String
)

@Composable
internal fun PanelHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun PanelEmptyState(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun InlineErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
internal fun DetailBlock(
    title: String,
    body: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun ToolbarTextFilter(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics {
            contentDescription = accessibilityDescription
        },
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) }
    )
}

@Composable
internal fun AdminMetricsRow(metrics: ImmutableList<AdminMetric>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        metrics.forEach { metric ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun <T> FilterGroup(
    title: String,
    options: Iterable<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    optionContentDescription: ((label: String, value: T) -> String)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (label, value) ->
                FilterChip(
                    modifier = optionContentDescription?.let { descriptionProvider ->
                        Modifier.semantics {
                            contentDescription = descriptionProvider(label, value)
                        }
                    } ?: Modifier,
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}
