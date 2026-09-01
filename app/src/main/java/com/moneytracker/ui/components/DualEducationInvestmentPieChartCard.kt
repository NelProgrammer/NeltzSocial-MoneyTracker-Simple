package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.ui.theme.EducationColor
import com.moneytracker.ui.theme.InvestmentColor
import com.moneytracker.util.CurrencyUtils

enum class ChartPalette(val title: String, val eduColors: List<Color>, val invColors: List<Color>) {
    CLASSIC(
        title = "Classic Modern",
        eduColors = listOf(Color(0xFF7B1FA2), Color(0xFF9C27B0), Color(0xFFBA68C8), Color(0xFFE1BEE7)),
        invColors = listOf(Color(0xFF1565C0), Color(0xFF1E88E5), Color(0xFF42A5F5), Color(0xFF90CAF9))
    ),
    VIVID(
        title = "Vivid Contrast",
        eduColors = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC), Color(0xFF8E24AA), Color(0xFFCE93D8)),
        invColors = listOf(Color(0xFF0D47A1), Color(0xFF0288D1), Color(0xFF00ACC1), Color(0xFF80DEEA))
    ),
    FOREST_OCEAN(
        title = "Forest & Ocean",
        eduColors = listOf(Color(0xFF2E7D32), Color(0xFF43A047), Color(0xFF66BB6A), Color(0xFFA5D6A7)),
        invColors = listOf(Color(0xFF00695C), Color(0xFF00897B), Color(0xFF26A69A), Color(0xFF80CBC4))
    ),
    SUNSET_NEON(
        title = "Sunset & Neon",
        eduColors = listOf(Color(0xFFC2185B), Color(0xFFE91E63), Color(0xFFF06292), Color(0xFFF8BBD0)),
        invColors = listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFB74D), Color(0xFFFFE0B2))
    )
}

/**
 * Unified Dual Education & Investment Analytics Card
 *
 * Highlights:
 * 1. Dual Donut Charts: Education (Left) & Investment (Right).
 * 2. 2x2 Top-4 Visible Legend by default with Expandable Card for remaining items.
 * 3. Color scheme customizer (via palette dropdown selector).
 * 4. Interactive Donut Arc slice clicking (filters transactions/dashboard).
 */
@Composable
fun DualEducationInvestmentPieChartCard(
    title: String = "Education & Investment Breakdown",
    educationSummaries: List<CategorySummary>,
    educationDetailSummaries: List<CategorySummary> = emptyList(),
    educationTotal: Double,
    investmentSummaries: List<CategorySummary>,
    investmentDetailSummaries: List<CategorySummary> = emptyList(),
    investmentTotal: Double,
    onSliceClick: ((categoryName: String, subCategoryName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var eduViewMode by remember { mutableStateOf(PieBreakdownMode.SUBCATEGORY) }
    var invViewMode by remember { mutableStateOf(PieBreakdownMode.SUBCATEGORY) }
    var isExpandedLegends by remember { mutableStateOf(false) }
    var selectedPalette by remember { mutableStateOf(ChartPalette.CLASSIC) }
    var showPaletteMenu by remember { mutableStateOf(false) }

    val hasEduDetails = educationDetailSummaries.isNotEmpty()
    val hasInvDetails = investmentDetailSummaries.isNotEmpty()

    val activeEduSummaries = if (eduViewMode == PieBreakdownMode.DETAIL && hasEduDetails) educationDetailSummaries else educationSummaries
    val activeInvSummaries = if (invViewMode == PieBreakdownMode.DETAIL && hasInvDetails) investmentDetailSummaries else investmentSummaries

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title & Palette Customizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Side-by-side comparative allocation & legends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Palette Customizer
                Box {
                    IconButton(
                        onClick = { showPaletteMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Change Color Scheme",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPaletteMenu,
                        onDismissRequest = { showPaletteMenu = false }
                    ) {
                        ChartPalette.values().forEach { palette ->
                            DropdownMenuItem(
                                text = { Text(palette.title, fontWeight = if (palette == selectedPalette) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedPalette = palette
                                    showPaletteMenu = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Dual Donut Charts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Education Donut
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Education",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EducationColor
                    )
                    Text(
                        text = CurrencyUtils.format(educationTotal),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    InteractiveDonutCanvas(
                        summaries = activeEduSummaries,
                        total = educationTotal,
                        palette = selectedPalette.eduColors,
                        emptyMessage = "No Education Data",
                        onSliceClick = { sub -> onSliceClick?.invoke("Education", sub) }
                    )

                    if (hasEduDetails) {
                        TextButton(
                            onClick = {
                                eduViewMode = if (eduViewMode == PieBreakdownMode.SUBCATEGORY) PieBreakdownMode.DETAIL else PieBreakdownMode.SUBCATEGORY
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (eduViewMode == PieBreakdownMode.SUBCATEGORY) "Show Details" else "Show Subs",
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right: Investment Donut
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Investment",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = InvestmentColor
                    )
                    Text(
                        text = CurrencyUtils.format(investmentTotal),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    InteractiveDonutCanvas(
                        summaries = activeInvSummaries,
                        total = investmentTotal,
                        palette = selectedPalette.invColors,
                        emptyMessage = "No Investment Data",
                        onSliceClick = { sub -> onSliceClick?.invoke("Investment", sub) }
                    )

                    if (hasInvDetails) {
                        TextButton(
                            onClick = {
                                invViewMode = if (invViewMode == PieBreakdownMode.SUBCATEGORY) PieBreakdownMode.DETAIL else PieBreakdownMode.SUBCATEGORY
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (invViewMode == PieBreakdownMode.SUBCATEGORY) "Show Details" else "Show Subs",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // =========================================================
            // 2x2 TOP-4 VISIBLE LEGENDS BY DEFAULT + EXPANDABLE CARD
            // Left Column: Education Legends | Right Column: Investment Legends
            // =========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Legends",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Expand / Collapse Toggle for remaining items
                val totalItemsCount = activeEduSummaries.size + activeInvSummaries.size
                if (totalItemsCount > 4) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isExpandedLegends = !isExpandedLegends }
                    ) {
                        Text(
                            text = if (isExpandedLegends) "Show Less" else "Show All ($totalItemsCount)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isExpandedLegends) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Top-4 Visible 2x2 Grid Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Education Legends Column (Top 2 items visible by default)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val eduDisplayList = if (isExpandedLegends) activeEduSummaries else activeEduSummaries.take(2)
                    if (eduDisplayList.isEmpty()) {
                        Text("-", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        eduDisplayList.forEachIndexed { index, summary ->
                            val color = selectedPalette.eduColors[index % selectedPalette.eduColors.size]
                            LegendItemRow(
                                title = summary.categoryName,
                                amount = summary.total,
                                totalAmount = educationTotal,
                                color = color,
                                onClick = { onSliceClick?.invoke("Education", summary.categoryName) }
                            )
                        }
                    }
                }

                // Investment Legends Column (Top 2 items visible by default)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val invDisplayList = if (isExpandedLegends) activeInvSummaries else activeInvSummaries.take(2)
                    if (invDisplayList.isEmpty()) {
                        Text("-", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        invDisplayList.forEachIndexed { index, summary ->
                            val color = selectedPalette.invColors[index % selectedPalette.invColors.size]
                            LegendItemRow(
                                title = summary.categoryName,
                                amount = summary.total,
                                totalAmount = investmentTotal,
                                color = color,
                                onClick = { onSliceClick?.invoke("Investment", summary.categoryName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveDonutCanvas(
    summaries: List<CategorySummary>,
    total: Double,
    palette: List<Color>,
    emptyMessage: String,
    onSliceClick: (subCategory: String) -> Unit
) {
    if (summaries.isEmpty() || total <= 0) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    style = Stroke(width = 14.dp.toPx())
                )
            }
            Text(
                text = "0%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(100.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Click defaults to top summary slice if clicked generally
                        summaries.firstOrNull()?.categoryName?.let { onSliceClick(it) }
                    }
            ) {
                var startAngle = -90f
                val strokeWidth = 16.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
                val arcSize = Size(radius * 2, radius * 2)

                summaries.forEachIndexed { index, summary ->
                    val sweepAngle = ((summary.total / total) * 360f).toFloat().coerceAtLeast(1f)
                    val color = palette[index % palette.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }

            Text(
                text = "${summaries.size} Subs",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LegendItemRow(
    title: String,
    amount: Double,
    totalAmount: Double,
    color: Color,
    onClick: () -> Unit
) {
    val pct = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${CurrencyUtils.format(amount)} (${String.format("%.0f", pct * 100)}%)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}
