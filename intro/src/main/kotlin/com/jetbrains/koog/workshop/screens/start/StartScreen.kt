package com.jetbrains.koog.workshop.screens.start

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.koog.workshop.theme.AppDimension


@Composable
fun StartScreen(viewModel: StartViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    StartScreenContent(
        cards = uiState.demoCards,
        isApiKeyConfigured = uiState.isApiKeyConfigured,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun StartScreenContent(
    cards: List<CardItem>,
    isApiKeyConfigured: Boolean,
    onEvent: (StartUiEvents) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimension.spacingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Koog Agents Workshop",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(AppDimension.spacingExtraSmall))
                    Text(
                        text = "Learn how to build agents in Kotlin with Koog",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isApiKeyConfigured) {
                    IconButton(
                        onClick = { onEvent.invoke(StartUiEvents.Settings) },
                        modifier = Modifier
                            .size(AppDimension.iconButtonSizeMedium)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Surface(
                        onClick = { onEvent.invoke(StartUiEvents.Settings) },
                        shape = RoundedCornerShape(AppDimension.radiusMedium),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = AppDimension.spacingMedium,
                                vertical = AppDimension.spacingSmall,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "No API key configured — tap to set up",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimension.spacingLarge))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingLarge),
            ) {
                cards.forEach { card ->
                    AgentCard(
                        card = card,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            card.agentDemoRoute?.let { demoRoute ->
                                onEvent.invoke(StartUiEvents.AgentDemo(demoRoute))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentCard(
    card: CardItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colorScheme = MaterialTheme.colorScheme
    val scale by animateFloatAsState(if (isHovered) 1.02f else 1f)
    val elevation by animateDpAsState(if (isHovered) AppDimension.elevationMedium else AppDimension.elevationNone)
    val borderColor by animateColorAsState(if (isHovered) colorScheme.primary else colorScheme.outlineVariant)

    ElevatedCard(
        modifier = modifier
            .scale(scale)
            .hoverable(interactionSource),
        shape = RoundedCornerShape(AppDimension.radiusExtraLarge),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent,
            contentColor = colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = elevation
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.surfaceContainer,
                            colorScheme.surfaceContainerLowest
                        )
                    ),
                    shape = RoundedCornerShape(AppDimension.radiusExtraLarge)
                )
                .border(1.dp, borderColor, RoundedCornerShape(AppDimension.radiusExtraLarge))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimension.spacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                if (card.imageRes != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 0.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Image(
                            painter = painterResource(card.imageRes),
                            contentDescription = card.title,
                            modifier = Modifier.fillMaxSize(0.9f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Text(
                    text = card.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppDimension.spacingSmall))

                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 20.sp
                    ),
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppDimension.spacingXXXXLarge))
            }
        }
    }
}
