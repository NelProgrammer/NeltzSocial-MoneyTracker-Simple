package com.moneytracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneytracker.util.DateUtils
import com.moneytracker.util.SettingsManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PayMonthFilterHeader(
    selectedPayMonthDate: LocalDate,
    onPayMonthSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isAnchorCenteringMode: Boolean = false
) {
    val payDateDay = SettingsManager.getPayDateDay()
    val currentPayMonthDate = remember(payDateDay) {
        DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay)
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    // Generate list of candidate PayMonths (-6 months to +12 months around currentPayMonthDate)
    val availablePayMonths = remember(currentPayMonthDate) {
        (-6..12).map { offset ->
            currentPayMonthDate.plusMonths(offset.toLong())
        }
    }

    // Determine index for segmented control (0 = Prev, 1 = Current, 2 = Next) relative to currentPayMonthDate
    val segmentedIndex = when (selectedPayMonthDate) {
        currentPayMonthDate.minusMonths(1) -> 0
        currentPayMonthDate -> 1
        currentPayMonthDate.plusMonths(1) -> 2
        else -> -1
    }

    Card(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "PayMonth Filter",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isAnchorCenteringMode) "Anchor Month" else "PayMonth",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Select Segmented Buttons (Prev, Curr, Next)
                SingleChoiceSegmentedButtonRow {
                    val labels = listOf("Prev", "Curr", "Next")
                    labels.forEachIndexed { index, label ->
                        val targetDate = when (index) {
                            0 -> currentPayMonthDate.minusMonths(1)
                            1 -> currentPayMonthDate
                            else -> currentPayMonthDate.plusMonths(1)
                        }
                        SegmentedButton(
                            selected = segmentedIndex == index,
                            onClick = { onPayMonthSelected(targetDate) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Dropdown Button (Month Picker) positioned to the right
                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = formatPayMonthShort(selectedPayMonthDate, payDateDay),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Month"
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        availablePayMonths.forEach { candidateDate ->
                            val isSelected = candidateDate == selectedPayMonthDate
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = formatPayMonthFull(candidateDate, payDateDay),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onPayMonthSelected(candidateDate)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatPayMonthShort(startDate: LocalDate, payDateDay: Int): String {
    val endDate = startDate.plusMonths(1).minusDays(1)
    val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    return "${endDate.format(monthFormatter)}"
}

private fun formatPayMonthFull(startDate: LocalDate, payDateDay: Int): String {
    val endDate = startDate.plusMonths(1).minusDays(1)
    val dayFormatter = DateTimeFormatter.ofPattern("d MMM")
    val yearFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    return "${startDate.format(dayFormatter)} - ${endDate.format(dayFormatter)} (${endDate.format(yearFormatter)})"
}
