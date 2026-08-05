package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.CategorySummaryTable
import com.moneytracker.ui.components.PayMonthFilterHeader
import com.moneytracker.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryTableScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues
) {
    val subCategorySummaries by viewModel.subCategorySummaries.collectAsState()
    val selectedPayMonthDate by viewModel.selectedPayMonthDate.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                screenTitle = "Dashboard Breakdown Table",
                showBack = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PayMonth Filter Header
            PayMonthFilterHeader(
                selectedPayMonthDate = selectedPayMonthDate,
                onPayMonthSelected = { viewModel.setPayMonth(it) }
            )

            // Full-screen Sub-Category Breakdown Table
            CategorySummaryTable(
                summaries = subCategorySummaries,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
