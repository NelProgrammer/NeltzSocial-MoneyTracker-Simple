package com.moneytracker.util

import android.content.Context
import com.moneytracker.data.local.MoneyTrackerDatabase
import com.moneytracker.data.local.ProfileSeeder
import com.moneytracker.data.local.entity.ProfileEntity
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProfileManager {
    private const val PREFS_NAME = "money_tracker_profile_session"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile: StateFlow<ProfileEntity?> = _activeProfile.asStateFlow()

    suspend fun initSession(
        context: Context,
        database: MoneyTrackerDatabase,
        repository: TransactionRepository
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Initial Launch check: Ensure profile "Ryu" and Guest profile exist
        var ryuProfile = database.profileDao().getByUsername("Ryu")
        if (ryuProfile == null) {
            val ryuId = database.profileDao().insert(
                ProfileEntity(
                    username = "Ryu",
                    isGuest = false,
                    isPasswordProtected = false
                )
            )
            ryuProfile = database.profileDao().getById(ryuId)
        }

        // Seed profile "Ryu" with 41 items if not already seeded
        if (ryuProfile != null) {
            ProfileSeeder.seedProfileIfEmpty(context, database, repository, ryuProfile.id, isGuest = false)
        }

        var guestProfile = database.profileDao().getGuestProfile()
        if (guestProfile == null) {
            val guestId = database.profileDao().insert(
                ProfileEntity(
                    username = "Guest",
                    isGuest = true,
                    isPasswordProtected = false
                )
            )
            guestProfile = database.profileDao().getById(guestId)
        }

        // Seed Guest profile with 41 items if not already seeded
        if (guestProfile != null) {
            ProfileSeeder.seedProfileIfEmpty(context, database, repository, guestProfile.id, isGuest = true)
        }

        // 2. Load active session
        val savedId = prefs.getLong(KEY_ACTIVE_PROFILE_ID, -1L)
        if (savedId != -1L) {
            val profile = database.profileDao().getById(savedId)
            if (profile != null) {
                _activeProfile.value = profile
                return
            }
        }

        // Default to Guest profile on startup if no active session
        if (guestProfile != null) {
            setActiveProfile(context, guestProfile)
        }
    }

    fun setActiveProfile(context: Context, profile: ProfileEntity?) {
        _activeProfile.value = profile
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (profile != null) {
            prefs.edit().putLong(KEY_ACTIVE_PROFILE_ID, profile.id).apply()
        } else {
            prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
        }
    }

    fun logout(context: Context) {
        setActiveProfile(context, null)
    }
}
