package com.moneytracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.ProfileEntity
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.ProfileManager
import com.moneytracker.util.ProfileValidator
import com.moneytracker.util.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    val activeProfile: StateFlow<ProfileEntity?> = ProfileManager.activeProfile

    // Filter profiles based on isRyuHidden setting
    val availableProfiles: StateFlow<List<ProfileEntity>> = combine(
        repository.observeAllProfiles(),
        SettingsManager.settings
    ) { profiles, settings ->
        if (settings.isRyuHidden) {
            profiles.filter { !it.username.equals("Ryu", ignoreCase = true) }
        } else {
            profiles
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val guestProfile: StateFlow<ProfileEntity?> = repository
        .observeAllProfiles()
        .combine(SettingsManager.settings) { list, _ ->
            list.find { it.isGuest }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun login(context: Context, profile: ProfileEntity, passwordInput: String? = null): Boolean {
        if (profile.isPasswordProtected) {
            if (passwordInput.isNullOrBlank() || profile.passwordHash != passwordInput) {
                return false // Invalid password
            }
        }
        ProfileManager.setActiveProfile(context, profile)
        return true
    }

    fun logout(context: Context) {
        ProfileManager.logout(context)
    }

    fun createPermanentProfile(
        context: Context,
        username: String,
        isPasswordProtected: Boolean,
        passwordInput: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val usernameError = ProfileValidator.validateUsername(username)
            if (usernameError != null) {
                onError(usernameError)
                return@launch
            }

            if (isPasswordProtected) {
                val passwordError = ProfileValidator.validatePassword(passwordInput ?: "")
                if (passwordError != null) {
                    onError(passwordError)
                    return@launch
                }
            }

            val newProfile = ProfileEntity(
                username = username,
                isGuest = false,
                isPasswordProtected = isPasswordProtected,
                passwordHash = if (isPasswordProtected) passwordInput else null
            )
            val newId = repository.saveProfile(newProfile)
            val created = repository.observeAllProfiles()
            ProfileManager.setActiveProfile(context, newProfile.copy(id = newId))
            onSuccess()
        }
    }

    fun convertGuestProfile(
        context: Context,
        newUsername: String,
        isPasswordProtected: Boolean,
        passwordInput: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val currentGuest = guestProfile.value
            if (currentGuest == null) {
                onError("No active guest profile found to convert.")
                return@launch
            }

            val usernameError = ProfileValidator.validateUsername(newUsername)
            if (usernameError != null) {
                onError(usernameError)
                return@launch
            }

            if (isPasswordProtected) {
                val passwordError = ProfileValidator.validatePassword(passwordInput ?: "")
                if (passwordError != null) {
                    onError(passwordError)
                    return@launch
                }
            }

            val success = repository.convertGuestToPermanent(
                guestId = currentGuest.id,
                newUsername = newUsername,
                isPasswordProtected = isPasswordProtected,
                passwordHash = if (isPasswordProtected) passwordInput else null
            )

            if (success) {
                val updatedProfile = currentGuest.copy(
                    username = newUsername,
                    isGuest = false,
                    isPasswordProtected = isPasswordProtected,
                    passwordHash = if (isPasswordProtected) passwordInput else null
                )
                ProfileManager.setActiveProfile(context, updatedProfile)
                onSuccess()
            } else {
                onError("Failed to convert guest profile.")
            }
        }
    }

    fun updateProfileSettings(
        context: Context,
        profile: ProfileEntity,
        newUsername: String,
        isPasswordProtected: Boolean,
        newPasswordInput: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val usernameError = ProfileValidator.validateUsername(newUsername)
            if (usernameError != null) {
                onError(usernameError)
                return@launch
            }

            if (isPasswordProtected) {
                val passwordError = ProfileValidator.validatePassword(newPasswordInput ?: "")
                if (passwordError != null) {
                    onError(passwordError)
                    return@launch
                }
            }

            val updated = profile.copy(
                username = newUsername,
                isPasswordProtected = isPasswordProtected,
                passwordHash = if (isPasswordProtected) newPasswordInput else null
            )
            repository.saveProfile(updated)
            if (activeProfile.value?.id == profile.id) {
                ProfileManager.setActiveProfile(context, updated)
            }
            onSuccess()
        }
    }
}
