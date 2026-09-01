package com.moneytracker.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemePalette(val displayName: String) {
    EMERALD_GREEN("Emerald Green"),
    OCEAN_BLUE("Ocean Blue"),
    ROYAL_VIOLET("Royal Violet"),
    SUNSET_AMBER("Sunset Amber")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

data class UserSettings(
    val payDateDay: Int = 20,
    val cutoffDay: Int = 18,
    val morningCutoffHour: Int = 12,
    val isRyuHidden: Boolean = true,
    val themePalette: AppThemePalette = AppThemePalette.EMERALD_GREEN,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

object SettingsManager {
    private const val PREFS_NAME = "money_tracker_user_settings"
    private const val KEY_PAY_DATE_DAY = "pay_date_day"
    private const val KEY_CUTOFF_DAY = "cutoff_day"
    private const val KEY_MORNING_CUTOFF_HOUR = "morning_cutoff_hour"
    private const val KEY_IS_RYU_HIDDEN = "is_ryu_hidden"
    private const val KEY_THEME_PALETTE = "theme_palette"
    private const val KEY_THEME_MODE = "theme_mode"

    private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val payDate = prefs?.getInt(KEY_PAY_DATE_DAY, 20) ?: 20
            val cutoff = prefs?.getInt(KEY_CUTOFF_DAY, 18) ?: 18
            val morningHour = prefs?.getInt(KEY_MORNING_CUTOFF_HOUR, 12) ?: 12
            val isRyuHidden = prefs?.getBoolean(KEY_IS_RYU_HIDDEN, true) ?: true
            val paletteName = prefs?.getString(KEY_THEME_PALETTE, AppThemePalette.EMERALD_GREEN.name)
            val palette = try {
                AppThemePalette.valueOf(paletteName ?: AppThemePalette.EMERALD_GREEN.name)
            } catch (e: Exception) {
                AppThemePalette.EMERALD_GREEN
            }
            val modeName = prefs?.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
            val mode = try {
                AppThemeMode.valueOf(modeName ?: AppThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }

            _settings.value = UserSettings(
                payDateDay = payDate,
                cutoffDay = cutoff,
                morningCutoffHour = morningHour,
                isRyuHidden = isRyuHidden,
                themePalette = palette,
                themeMode = mode
            )
        }
    }

    fun updatePayDateDay(day: Int) {
        val validDay = day.coerceIn(1, 31)
        val current = _settings.value
        _settings.value = current.copy(payDateDay = validDay)
        prefs?.edit()?.putInt(KEY_PAY_DATE_DAY, validDay)?.apply()
    }

    fun updateCutoffDay(day: Int) {
        val validDay = day.coerceIn(1, 31)
        val current = _settings.value
        _settings.value = current.copy(cutoffDay = validDay)
        prefs?.edit()?.putInt(KEY_CUTOFF_DAY, validDay)?.apply()
    }

    fun updateMorningCutoffHour(hour: Int) {
        val validHour = hour.coerceIn(1, 23)
        val current = _settings.value
        _settings.value = current.copy(morningCutoffHour = validHour)
        prefs?.edit()?.putInt(KEY_MORNING_CUTOFF_HOUR, validHour)?.apply()
    }

    fun updateIsRyuHidden(hidden: Boolean) {
        val current = _settings.value
        _settings.value = current.copy(isRyuHidden = hidden)
        prefs?.edit()?.putBoolean(KEY_IS_RYU_HIDDEN, hidden)?.apply()
    }

    fun updateThemePalette(palette: AppThemePalette) {
        val current = _settings.value
        _settings.value = current.copy(themePalette = palette)
        prefs?.edit()?.putString(KEY_THEME_PALETTE, palette.name)?.apply()
    }

    fun updateThemeMode(mode: AppThemeMode) {
        val current = _settings.value
        _settings.value = current.copy(themeMode = mode)
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    // Generic Grid Settings Persistence
    fun saveGridString(gridId: String, key: String, value: String) {
        prefs?.edit()?.putString("grid_${gridId}_$key", value)?.apply()
    }

    fun getGridString(gridId: String, key: String, default: String = ""): String {
        return prefs?.getString("grid_${gridId}_$key", default) ?: default
    }

    fun saveGridInt(gridId: String, key: String, value: Int) {
        prefs?.edit()?.putInt("grid_${gridId}_$key", value)?.apply()
    }

    fun getGridInt(gridId: String, key: String, default: Int): Int {
        return prefs?.getInt("grid_${gridId}_$key", default) ?: default
    }

    fun saveGridBoolean(gridId: String, key: String, value: Boolean) {
        prefs?.edit()?.putBoolean("grid_${gridId}_$key", value)?.apply()
    }

    fun getGridBoolean(gridId: String, key: String, default: Boolean): Boolean {
        return prefs?.getBoolean("grid_${gridId}_$key", default) ?: default
    }

    fun getAllGridStringMap(gridId: String): Map<String, String> {
        val prefix = "grid_${gridId}_"
        val all = prefs?.all ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        for ((k, v) in all) {
            if (k.startsWith(prefix) && v is String) {
                result[k.removePrefix(prefix)] = v
            }
        }
        return result
    }

    fun getPayDateDay(): Int = _settings.value.payDateDay
    fun getCutoffDay(): Int = _settings.value.cutoffDay
    fun getMorningCutoffHour(): Int = _settings.value.morningCutoffHour
    fun isRyuHidden(): Boolean = _settings.value.isRyuHidden
    fun getThemePalette(): AppThemePalette = _settings.value.themePalette
    fun getThemeMode(): AppThemeMode = _settings.value.themeMode
}
