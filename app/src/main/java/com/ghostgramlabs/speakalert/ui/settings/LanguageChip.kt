package com.ghostgramlabs.speakalert.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Chip for the in-app language picker. Unlike [SnoozeOptionChip] it is never stretched with
 * `weight`: it sizes to its label (with a minimum only), so scripts of very different widths
 * (Devanagari, Arabic, Latin) all render without clipping and wrap naturally inside a FlowRow.
 */
@Composable
fun LanguageChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = MaterialTheme.colorScheme

    val gradientTop by animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer else colors.surface.copy(alpha = 0.92f),
        animationSpec = tween(durationMillis = 220),
        label = "languageChipTop"
    )
    val gradientBottom by animateColorAsState(
        targetValue = if (isSelected) {
            lerp(colors.primaryContainer, colors.primary, 0.28f)
        } else {
            colors.surface.copy(alpha = 0.92f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "languageChipBottom"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            colors.primary.copy(alpha = 0.35f)
        } else {
            colors.outlineVariant.copy(alpha = 0.8f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "languageChipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
        animationSpec = tween(durationMillis = 220),
        label = "languageChipText"
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "languageChipElevation"
    )

    Box(
        modifier = modifier
            // Minimums only: width tracks the label, height grows with font scale. 48dp keeps
            // the touch target accessible even for short labels.
            .defaultMinSize(minWidth = 72.dp, minHeight = 48.dp)
            .shadow(elevation, shape, ambientColor = colors.primary, spotColor = colors.primary)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}
