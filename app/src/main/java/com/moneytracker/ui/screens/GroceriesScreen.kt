package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

@Composable
fun GroceriesScreen(contentPadding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Groceries Budget – Coming Soon",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
