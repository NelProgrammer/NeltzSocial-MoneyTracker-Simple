package com.moneytracker.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Extension function to safely navigate to a route without throwing an IllegalArgumentException
 * if the navigation action is invalid (e.g., navigating to the same destination multiple times).
 */
fun NavController.navigateSafe(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    try {
        this.navigate(route, builder)
    } catch (e: IllegalArgumentException) {
        // Log the exception if needed; ignore to prevent crashes from rapid double clicks.
    }
}
