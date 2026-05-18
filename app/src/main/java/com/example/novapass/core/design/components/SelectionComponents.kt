package com.example.novapass.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.novapass.core.design.theme.NovaBrushes
import com.example.novapass.core.design.theme.NovaColors

@Composable
fun TicketCategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
    val shape = RoundedCornerShape(12.dp)
    val density = LocalDensity.current
    val chipBounds = remember(categories) { mutableStateMapOf<Int, Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp>>() }
    val selectedBounds = chipBounds[selectedIndex]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val indicatorOffset by animateDpAsState(
            targetValue = selectedBounds?.first ?: 0.dp,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "categoryIndicatorOffset"
        )
        val indicatorWidth by animateDpAsState(
            targetValue = selectedBounds?.second ?: 0.dp,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "categoryIndicatorWidth"
        )

        if (indicatorWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(NovaBrushes.GoldGradient)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            categories.forEachIndexed { index, category ->
                val selected = category == selectedCategory
                val textColor by animateColorAsState(
                    targetValue = if (selected) NovaColors.BackgroundPrimary else NovaColors.TextSecondary,
                    animationSpec = tween(durationMillis = 180),
                    label = "categoryTextColor"
                )
                val interactionSource = remember(category) { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(shape)
                        .background(if (selected) NovaColors.Transparent else NovaColors.GlassLight)
                        .border(
                            width = 1.dp,
                            color = if (selected) NovaColors.Transparent else NovaColors.BorderSubtle,
                            shape = shape
                        )
                        .onGloballyPositioned { coordinates ->
                            val offsetX = with(density) { coordinates.positionInParent().x.toDp() }
                            val width = with(density) { coordinates.size.width.toDp() }
                            chipBounds[index] = offsetX to width
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current
                        ) { onCategorySelected(category) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            color = textColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
