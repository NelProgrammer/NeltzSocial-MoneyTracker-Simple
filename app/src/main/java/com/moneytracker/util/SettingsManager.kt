package com.moneytracker.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val payDateDay: Int = 20,
    val cutoffDay: Int = 18
)

object SettingsManager {
    private const val PREFS_NAME = "money_tracker_user_settings"
    private const val KEY_PAY_DATE_DAY = "pay_date_day"
    private const val KEY_CUTOFF_DAY = "cutoff_day"

    private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val payDate = prefs?.getInt(KEY_PAY_DATE_DAY, 20) ?: 20
            val cutoff = prefs?.getInt(KEY_CUTOFF_DAY, 18) ?: 18
            _settings.value = UserSettings(payDateDay = payDate, cutoffDay = cutoff)
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

    fun getPayDateDay(): Int = _settings.value.payDateDay
    fun getCutoffDay(): Int = _settings.value.cutoffDay
}
