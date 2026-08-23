package com.moneytracker.ui.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.moneytracker.util.CurrencyUtils

enum class PieBreakdownMode {
    SUBCATEGORY,
    DETAIL
}

fun getSliceColor(index: Int, baseColor: Color): Color {
    val palette = when (baseColor) {
        com.moneytracker.ui.theme.IncomeColor -> listOf(
            Color(0xFF2E7D32), Color(0xFF00897B), Color(0xFF00ACC1), Color(0xFF43A047), Color(0xFF66BB6A), Color(0xFF26A69A)
        )
        com.moneytracker.ui.theme.InvestmentColor -> listOf(
            Color(0xFF1565C0), Color(0xFF283593), Color(0xFF0288D1), Color(0xFF3F51B5), Color(0xFF5C6BC0), Color(0xFF0097A7)
        )
        com.moneytracker.ui.theme.EducationColor -> listOf(
            Color(0xFF6A1B9A), Color(0xFF4A148C), Color(0xFF8E24AA), Color(0xFFAB47BC), Color(0xFF7B1FA2), Color(0xFFBA68C8)
        )
        com.moneytracker.ui.theme.ExpenseColor -> listOf(
            Color(0xFFC62828), Color(0xFFEF6C00), Color(0xFFD81B60), Color(0xFFE65100), Color(0xFFF4511E), Color(0xFF8E24AA)
        )
        else -> {
            val r = baseColor.red
            val g = baseColor.green
            val b = baseColor.blue
            listOf(
                baseColor,
                Color(red = (r * 0.82f).coerceIn(0f, 1f), green = (g * 0.82f).coerceIn(0f, 1f), blue = (b * 0.82f).coerceIn(0f, 1f)),
                Color(red = (r * 1.18f).coerceIn(0f, 1f), green = (g * 1.18f).coerceIn(0f, 1f), blue = (b * 1.18f).coerceIn(0f, 1f)),
                Color(red = (r * 0.65f).coerceIn(0f, 1f), green = (g * 0.65f).coerceIn(0f, 1f), blue = (b * 0.65f).coerceIn(0f, 1f)),
                Color(red = (r * 1.35f).coerceIn(0f, 1f), green = (g * 1.35f).coerceIn(0f, 1f), blue = (b * 1.35f).coerceIn(0f, 1f)),
                Color(red = (r * 0.50f).coerceIn(0f, 1f), green = (g * 0.50f).coerceIn(0f, 1f), blue = (b * 0.50f).coerceIn(0f, 1f))
            )
        }
    }
    return palette[index % palette.size]
}

@Composable
private fun DonutChart(
    summaries: List<CategorySummary>,
    totalAmount: Double,
    baseColor: Color,
    emptyMessage: String,
    hasDetails: Boolean,
    viewMode: PieBreakdownMode,
    onToggleViewMode: () -> Unit,
    glowAlpha: Float,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sizeDp = if (isCompact) 100.dp else 125.dp
    val heightDp = if (isCompact) 110.dp else 140.dp
    val strokeWidthDp = if (isCompact) 14.dp else 20.dp

    if (summaries.isEmpty() || totalAmount <= 0.0) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = if (isCompact) 4.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(sizeDp)) {
                    val strokeWidth = strokeWidthDp.toPx()
                    drawArc(
                        color = baseColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isCompact) 9.sp else 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(sizeDp)) {
                val strokeWidth = strokeWidthDp.toPx()
                val glowInset = 7.dp.toPx()
                val mainArcTopLeft = Offset(glowInset, glowInset)
                val mainArcSize = Size(size.width - 2 * glowInset, size.height - 2 * glowInset)

                // Pass 1: Draw the solid category slices (pure solid fill)
                var startAngle = -90f
                summaries.forEachIndexed { index, summary ->
                    val sweepAngle = (summary.total / totalAmount * 360f).toFloat()
                    val color = summary.customColorHex?.let { Color(it) } ?: getSliceColor(index, baseColor)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = mainArcTopLeft,
                        size = mainArcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }

                // Pass 2: Draw Electric Neon Cyan glow mathematically aligned to the outer and inner edges of the doughnut
                val glowBorderWidth = 2.dp.toPx()
                val auraWidth = 4.5.dp.toPx()
                val halfStroke = strokeWidth / 2f

                val outerDiameter = mainArcSize.width + strokeWidth
                val outerTopLeft = Offset(glowInset - halfStroke, glowInset - halfStroke)
                val outerSize = Size(outerDiameter, outerDiameter)

                val innerDiameter = mainArcSize.width - strokeWidth
                val innerTopLeft = Offset(glowInset + halfStroke, glowInset + halfStroke)
                val innerSize = Size(innerDiameter, innerDiameter)

                var haloAngle = -90f
                summaries.forEach { summary ->
                    val sweepAngle = (summary.total / totalAmount * 360f).toFloat()
                    if (summary.isDebtFunding && sweepAngle > 0f) {
                        val glowAlphaVal = 0.8f + 0.2f * glowAlpha
                        val glowBorderColor = Color(0xFF00E5FF).copy(alpha = glowAlphaVal)
                        val glowAuraColor = Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.45f)

                        // 1. Outer Arc Glow (Centered precisely on the doughnut's outer boundary radius)
                        drawArc(
                            color = glowAuraColor,
                            startAngle = haloAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = outerTopLeft,
                            size = outerSize,
                            style = Stroke(width = auraWidth, cap = StrokeCap.Butt)
                        )
                        drawArc(
                            color = glowBorderColor,
                            startAngle = haloAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = outerTopLeft,
                            size = outerSize,
                            style = Stroke(width = glowBorderWidth, cap = StrokeCap.Butt)
                        )

                        // 2. Inner Arc Glow (Centered precisely on the doughnut's inner boundary radius)
                        drawArc(
                            color = glowAuraColor,
                            startAngle = haloAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = innerTopLeft,
                            size = innerSize,
                            style = Stroke(width = auraWidth, cap = StrokeCap.Butt)
                        )
                        drawArc(
                            color = glowBorderColor,
                            startAngle = haloAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = innerTopLeft,
                            size = innerSize,
                            style = Stroke(width = glowBorderWidth, cap = StrokeCap.Butt)
                        )
                    }
                    haloAngle += sweepAngle
                }
            }

            // Centered Clickable inside Donut
            if (hasDetails) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isCompact) 48.dp else 70.dp)
                        .clip(CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onToggleViewMode()
                        }
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (viewMode == PieBreakdownMode.DETAIL) "Sub-Cats" else "Details",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isCompact) 7.5.sp else 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                horizontal = if (isCompact) 4.dp else 6.dp,
                                vertical = if (isCompact) 2.dp else 3.dp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPieChartCard(
    title: String,
    summaries: List<CategorySummary>,
    detailSummaries: List<CategorySummary> = emptyList(),
    totalAmount: Double,
    baseColor: Color,
    emptyMessage: String,
    useTwoColumns: Boolean = true,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(PieBreakdownMode.SUBCATEGORY) }
    val hasDetails = detailSummaries.isNotEmpty()
    val activeSummaries = if (viewMode == PieBreakdownMode.DETAIL && hasDetails) detailSummaries else summaries

    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 10.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            // Header Title (Centered, bracketed text on second line)
            val bracketIndex = title.indexOfAny(charArrayOf('(', '['))
            val (mainTitle, subTitle) = if (bracketIndex > 0) {
                Pair(title.substring(0, bracketIndex).trim(), title.substring(bracketIndex).trim())
            } else {
                Pair(title, null)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mainTitle,
                    style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = baseColor,
                    textAlign = TextAlign.Center
                )
                if (subTitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subTitle,
                        style = if (isCompact) MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp) else MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = baseColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DonutChart(
                summaries = activeSummaries,
                totalAmount = totalAmount,
                baseColor = baseColor,
                emptyMessage = emptyMessage,
                hasDetails = hasDetails,
                viewMode = viewMode,
                onToggleViewMode = {
                    viewMode = if (viewMode == PieBreakdownMode.SUBCATEGORY) PieBreakdownMode.DETAIL else PieBreakdownMode.SUBCATEGORY
                },
                glowAlpha = glowAlpha,
                isCompact = isCompact
            )

            if (activeSummaries.isNotEmpty() && totalAmount > 0.0) {
                // Legend & Progress Items
                if (useTwoColumns) {
                    val indexedSummaries = activeSummaries.mapIndexed { idx, item -> Pair(idx, item) }
                    val rows = indexedSummaries.chunked(2)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { (index, summary) ->
                                    val color = summary.customColorHex?.let { Color(it) } ?: getSliceColor(index, baseColor)
                                    val pct = if (totalAmount > 0) (summary.total / totalAmount).toFloat() else 0f

                                    val debtBorder = if (summary.isDebtFunding) {
                                        BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = glowAlpha))
                                    } else null

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = debtBorder,
                                        color = if (summary.isDebtFunding) Color(0xFF00E5FF).copy(alpha = 0.08f) else Color.Transparent
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(if (summary.isDebtFunding) 4.dp else 0.dp),
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
                                                        text = summary.categoryName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${String.format("%.1f", pct * 100)}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = color
                                                )
                                            }
                                            Text(
                                                text = CurrencyUtils.format(summary.total),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            LinearProgressIndicator(
                                                progress = { pct.coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = color,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        activeSummaries.forEachIndexed { index, summary ->
                            val color = summary.customColorHex?.let { Color(it) } ?: getSliceColor(index, baseColor)
                            val pct = if (totalAmount > 0) (summary.total / totalAmount).toFloat() else 0f

                            val debtBorder = if (summary.isDebtFunding) {
                                BorderStroke(1.5.dp, Color(0xFFFF6D00).copy(alpha = glowAlpha))
                            } else null

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = debtBorder,
                                color = if (summary.isDebtFunding) Color(0xFFFF6D00).copy(alpha = 0.08f) else Color.Transparent
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (summary.isDebtFunding) 6.dp else 0.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = summary.categoryName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (summary.isDebtFunding) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFFF6D00).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Debt Funding",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = Color(0xFFFF6D00),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${CurrencyUtils.format(summary.total)} (${String.format("%.1f", pct * 100)}%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = color,
                                            maxLines = 1
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { pct.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DualIncomePieChartCard(
    title: String = "Income Sources and Utilization",
    sourceSummaries: List<CategorySummary>,
    sourceDetailSummaries: List<CategorySummary> = emptyList(),
    sourceTotal: Double,
    usageSummaries: List<CategorySummary>,
    usageDetailSummaries: List<CategorySummary> = emptyList(),
    usageTotal: Double,
    baseColor: Color = com.moneytracker.ui.theme.IncomeColor,
    emptySourceMessage: String = "No income recorded for this pay period.",
    emptyUsageMessage: String = "No usage recorded for this pay period.",
    modifier: Modifier = Modifier
) {
    var sourceViewMode by remember { mutableStateOf(PieBreakdownMode.SUBCATEGORY) }
    var usageViewMode by remember { mutableStateOf(PieBreakdownMode.SUBCATEGORY) }

    val hasSourceDetails = sourceDetailSummaries.isNotEmpty()
    val hasUsageDetails = usageDetailSummaries.isNotEmpty()

    val activeSourceSummaries = if (sourceViewMode == PieBreakdownMode.DETAIL && hasSourceDetails) sourceDetailSummaries else sourceSummaries
    val activeUsageSummaries = if (usageViewMode == PieBreakdownMode.DETAIL && hasUsageDetails) usageDetailSummaries else usageSummaries

    val infiniteTransition = rememberInfiniteTransition(label = "dualGlowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dualGlowAlpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Title (Centered, bracketed text on second line)
            val bracketIndex = title.indexOfAny(charArrayOf('(', '['))
            val (mainTitle, subTitle) = if (bracketIndex > 0) {
                Pair(title.substring(0, bracketIndex).trim(), title.substring(bracketIndex).trim())
            } else {
                Pair(title, null)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mainTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = baseColor,
                    textAlign = TextAlign.Center
                )
                if (subTitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = baseColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Side-by-Side Charts Row (Completely level with each other)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Chart: Income Sources
                DonutChart(
                    summaries = activeSourceSummaries,
                    totalAmount = sourceTotal,
                    baseColor = baseColor,
                    emptyMessage = emptySourceMessage,
                    hasDetails = hasSourceDetails,
                    viewMode = sourceViewMode,
                    onToggleViewMode = {
                        sourceViewMode = if (sourceViewMode == PieBreakdownMode.SUBCATEGORY) PieBreakdownMode.DETAIL else PieBreakdownMode.SUBCATEGORY
                    },
                    glowAlpha = glowAlpha,
                    isCompact = true,
                    modifier = Modifier.weight(1f)
                )

                // Right Chart: Funding & Utilization
                DonutChart(
                    summaries = activeUsageSummaries,
                    totalAmount = usageTotal,
                    baseColor = baseColor,
                    emptyMessage = emptyUsageMessage,
                    hasDetails = hasUsageDetails,
                    viewMode = usageViewMode,
                    onToggleViewMode = {
                        usageViewMode = if (usageViewMode == PieBreakdownMode.SUBCATEGORY) PieBreakdownMode.DETAIL else PieBreakdownMode.SUBCATEGORY
                    },
                    glowAlpha = glowAlpha,
                    isCompact = true,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 1. Income Sources Bar / Legend on Top (2 Columns if multiple items, single card if 1)
            if (activeSourceSummaries.isNotEmpty() && sourceTotal > 0) {
                if (activeSourceSummaries.size == 1) {
                    val summary = activeSourceSummaries.first()
                    val color = summary.customColorHex?.let { Color(it) } ?: getSliceColor(0, baseColor)
                    val pct = if (sourceTotal > 0) (summary.total / sourceTotal).toFloat() else 0f

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = summary.categoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${CurrencyUtils.format(summary.total)} (${String.format("%.1f", pct * 100)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pct.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    val indexedSources = activeSourceSummaries.mapIndexed { idx, item -> Pair(idx, item) }
                    val sourceRows = indexedSources.chunked(2)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sourceRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { (index, summary) ->
                                    val color = summary.customColorHex?.let { Color(it) } ?: getSliceColor(index, baseColor)
                                    val pct = if (sourceTotal > 0) (summary.total / sourceTotal).toFloat() else 0f

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        color = color.copy(alpha = 0.08f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
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
                                                        text = summary.categoryName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${String.format("%.1f", pct * 100)}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = color
                                                )
                                            }
                                            Text(
                                                text = CurrencyUtils.format(summary.total),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            LinearProgressIndicator(
                                                progress = { pct.coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = color,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Funding & Utilization Bars in 2 Columns Underneath (including Remaining Income & Debt Funding)
            if (activeUsageSummaries.isNotEmpty() && usageTotal > 0) {
                val indexedUsage = activeUsageSummaries.mapIndexed { idx, item -> Pair(idx, item) }
                val usageRows = indexedUsage.chunked(2)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    usageRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { (index, summary) ->
                                val color = summary.customColorHex?.let { Color(it) } ?: getSliceColor(index, baseColor)
                                val pct = if (usageTotal > 0) (summary.total / usageTotal).toFloat() else 0f

                                val debtBorder = if (summary.isDebtFunding) {
                                    BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = glowAlpha))
                                } else null

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = debtBorder,
                                    color = if (summary.isDebtFunding) Color(0xFF00E5FF).copy(alpha = 0.08f) else Color.Transparent
                                ) {
                                    Column(
                                        modifier = Modifier.padding(if (summary.isDebtFunding) 4.dp else 0.dp),
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
                                                    text = summary.categoryName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${String.format("%.1f", pct * 100)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = color
                                            )
                                        }
                                        Text(
                                            text = CurrencyUtils.format(summary.total),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = color,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        LinearProgressIndicator(
                                            progress = { pct.coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = color,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
