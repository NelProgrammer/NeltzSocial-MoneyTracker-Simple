package com.moneytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.moneytracker.util.AppThemeMode
import com.moneytracker.util.AppThemePalette
import com.moneytracker.util.SettingsManager

// Core Category Colors (Vibrant & High Contrast)
val IncomeColor: Color = Color(0xFF2E7D32)
val InvestmentColor: Color = Color(0xFF1565C0)
val EducationColor: Color = Color(0xFF8E24AA)
val ExpenseColor: Color = Color(0xFFC62828)

// Curated distinct color palette for custom categories
val CustomCategoryPalette: List<Color> = listOf(
    Color(0xFFFFB300), // Amber / Gold
    Color(0xFF00BFA5), // Vibrant Teal
    Color(0xFF3D5AFE), // Electric Indigo
    Color(0xFFFF4081), // Hot Pink / Magenta
    Color(0xFF00E5FF), // Radiant Cyan
    Color(0xFF76FF03), // Chartreuse
    Color(0xFFFF6D00), // Bright Orange
    Color(0xFF651FFF), // Deep Purple / Violet
    Color(0xFF00B0FF), // Sky Blue
    Color(0xFFFF5252), // Coral Red
    Color(0xFFAEEA00), // Lime Neon
    Color(0xFFD84315), // Rust / Terracotta
    Color(0xFF1DE9B6), // Mint / Turquoise
    Color(0xFF7C4DFF), // Royal Lavender
    Color(0xFFFFAB00), // Pure Amber
    Color(0xFF0091EA)  // Light Blue
)

fun getCategoryBaseColor(categoryName: String, fallbackType: com.moneytracker.data.local.entity.TransactionType? = null): Color {
    val clean = categoryName.trim().lowercase()
    return when {
        clean == "income" || (fallbackType == com.moneytracker.data.local.entity.TransactionType.INCOME && clean.isBlank()) -> IncomeColor
        clean == "investment" || (fallbackType == com.moneytracker.data.local.entity.TransactionType.INVESTMENT && clean.isBlank()) -> InvestmentColor
        clean == "education" || (fallbackType == com.moneytracker.data.local.entity.TransactionType.EDUCATION && clean.isBlank()) -> EducationColor
        clean == "expense" || (fallbackType == com.moneytracker.data.local.entity.TransactionType.EXPENSE && clean.isBlank()) -> ExpenseColor
        clean.isBlank() -> fallbackType?.let {
            when (it) {
                com.moneytracker.data.local.entity.TransactionType.INCOME -> IncomeColor
                com.moneytracker.data.local.entity.TransactionType.INVESTMENT -> InvestmentColor
                com.moneytracker.data.local.entity.TransactionType.EDUCATION -> EducationColor
                com.moneytracker.data.local.entity.TransactionType.EXPENSE -> ExpenseColor
            }
        } ?: ExpenseColor
        else -> {
            val hash = kotlin.math.abs(clean.hashCode())
            CustomCategoryPalette[hash % CustomCategoryPalette.size]
        }
    }
}

fun getCategoryColorHex(categoryName: String, fallbackType: com.moneytracker.data.local.entity.TransactionType? = null): Long {
    val color = getCategoryBaseColor(categoryName, fallbackType)
    val a = (color.alpha * 255.0f + 0.5f).toInt()
    val r = (color.red * 255.0f + 0.5f).toInt()
    val g = (color.green * 255.0f + 0.5f).toInt()
    val b = (color.blue * 255.0f + 0.5f).toInt()
    return (((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)).toLong() and 0xFFFFFFFFL
}

// 1. Emerald Green Palette
private val EmeraldLightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF388E3C),
    onSecondary = Color.White,
    background = Color(0xFFF6F8F6),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF37474F),
    error = ExpenseColor
)

private val EmeraldDarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF2E5E35),
    onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFFAED581),
    onSecondary = Color(0xFF1A3300),
    background = Color(0xFF121512),
    surface = Color(0xFF1E241E),
    surfaceVariant = Color(0xFF2B362B),
    onSurfaceVariant = Color(0xFFCFD8DC),
    error = Color(0xFFEF5350)
)

// 2. Ocean Blue Palette
private val OceanLightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    background = Color(0xFFF4F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3F2FD),
    onSurfaceVariant = Color(0xFF37474F),
    error = ExpenseColor
)

private val OceanDarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF002244),
    primaryContainer = Color(0xFF1E4976),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF00363A),
    background = Color(0xFF11161C),
    surface = Color(0xFF1B222B),
    surfaceVariant = Color(0xFF263342),
    onSurfaceVariant = Color(0xFFCFD8DC),
    error = Color(0xFFEF5350)
)

// 3. Royal Violet Palette
private val VioletLightColors = lightColorScheme(
    primary = Color(0xFF7B1FA2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF8E24AA),
    onSecondary = Color.White,
    background = Color(0xFFF9F5FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3E5F5),
    onSurfaceVariant = Color(0xFF37474F),
    error = ExpenseColor
)

private val VioletDarkColors = darkColorScheme(
    primary = Color(0xFFBA68C8),
    onPrimary = Color(0xFF330033),
    primaryContainer = Color(0xFF5A2A6E),
    onPrimaryContainer = Color(0xFFF3E5F5),
    secondary = Color(0xFFCE93D8),
    onSecondary = Color(0xFF3E124A),
    background = Color(0xFF18121A),
    surface = Color(0xFF241B28),
    surfaceVariant = Color(0xFF37273D),
    onSurfaceVariant = Color(0xFFCFD8DC),
    error = Color(0xFFEF5350)
)

// 4. Sunset Amber Palette
private val AmberLightColors = lightColorScheme(
    primary = Color(0xFFD84315),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCCBC),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFF57F17),
    onSecondary = Color.White,
    background = Color(0xFFFAF7F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFFBE9E7),
    onSurfaceVariant = Color(0xFF37474F),
    error = ExpenseColor
)

private val AmberDarkColors = darkColorScheme(
    primary = Color(0xFFFF8A65),
    onPrimary = Color(0xFF4A1400),
    primaryContainer = Color(0xFF6E3218),
    onPrimaryContainer = Color(0xFFFBE9E7),
    secondary = Color(0xFFFFD54F),
    onSecondary = Color(0xFF3E2723),
    background = Color(0xFF1A1412),
    surface = Color(0xFF281E1A),
    surfaceVariant = Color(0xFF3E2D26),
    onSurfaceVariant = Color(0xFFCFD8DC),
    error = Color(0xFFEF5350)
)

fun getThemeColorScheme(palette: AppThemePalette, isDark: Boolean): ColorScheme {
    return when (palette) {
        AppThemePalette.EMERALD_GREEN -> if (isDark) EmeraldDarkColors else EmeraldLightColors
        AppThemePalette.OCEAN_BLUE -> if (isDark) OceanDarkColors else OceanLightColors
        AppThemePalette.ROYAL_VIOLET -> if (isDark) VioletDarkColors else VioletLightColors
        AppThemePalette.SUNSET_AMBER -> if (isDark) AmberDarkColors else AmberLightColors
    }
}

@Composable
fun MoneyTrackerTheme(
    palette: AppThemePalette? = null,
    mode: AppThemeMode? = null,
    content: @Composable () -> Unit
) {
    val settings by SettingsManager.settings.collectAsState()
    val activePalette = palette ?: settings.themePalette
    val activeMode = mode ?: settings.themeMode
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (activeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = getThemeColorScheme(activePalette, isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
