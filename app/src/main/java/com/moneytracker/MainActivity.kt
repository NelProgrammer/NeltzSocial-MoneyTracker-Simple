package com.moneytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moneytracker.ui.navigation.MoneyTrackerNavHost
import com.moneytracker.ui.theme.MoneyTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as MoneyTrackerApp).repository
        setContent {
            MoneyTrackerTheme {
                MoneyTrackerNavHost(repository = repository)
            }
        }
    }
}
