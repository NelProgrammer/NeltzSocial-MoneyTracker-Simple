package com.moneytracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.theme.InvestmentColor
import com.moneytracker.util.CurrencyUtils

/**
 * Balance Card for Dashboard displaying Category Totals and Net Balance.
 * Formula: Income − Investment − Expense.
 */
import com.moneytracker.ui.theme.EducationColor

@Composable
fun BalanceCard(
    balance: Double,
    income: Double,
    investment: Double,
    education: Double = 0.0,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val absIncome = kotlin.math.abs(income)
    val absInvestment = kotlin.math.abs(investment)
    val absEducation = kotlin.math.abs(education)
    val absExpense = kotlin.math.abs(expense)
    val calculatedBalance = absIncome - absInvestment - absEducation - absExpense

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Net Balance Title & Amount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Net Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = CurrencyUtils.format(calculatedBalance),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (calculatedBalance >= 0) IncomeColor else ExpenseColor
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            // Category Totals Grid (Income +, Investment -, Education -, Expense -)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryBalanceItem(
                    label = "Income (+)",
                    amount = absIncome,
                    color = IncomeColor,
                    modifier = Modifier.weight(1f)
                )
                CategoryBalanceItem(
                    label = "Investment (−)",
                    amount = absInvestment,
                    color = InvestmentColor,
                    modifier = Modifier.weight(1f)
                )
                CategoryBalanceItem(
                    label = "Education (−)",
                    amount = absEducation,
                    color = EducationColor,
                    modifier = Modifier.weight(1f)
                )
                CategoryBalanceItem(
                    label = "Expense (−)",
                    amount = absExpense,
                    color = ExpenseColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryBalanceItem(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = CurrencyUtils.format(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}
