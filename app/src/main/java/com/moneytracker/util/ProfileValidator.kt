package com.moneytracker.util

object ProfileValidator {
    // Alphanumeric, #, @, _, no spaces
    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9#@_]{4,20}$")
    private val PASSWORD_REGEX = Regex("^[a-zA-Z0-9#@_]{4,10}$")

    fun validateUsername(username: String): String? {
        if (username.length < 4 || username.length > 20) {
            return "Username must be between 4 and 20 characters long."
        }
        if (!USERNAME_REGEX.matches(username)) {
            return "Username can only contain alphanumeric characters, #, @, and _ (no spaces)."
        }
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < 4 || password.length > 10) {
            return "Password must be between 4 and 10 characters long."
        }
        if (!PASSWORD_REGEX.matches(password)) {
            return "Password can only contain alphanumeric characters, #, @, and _ (no spaces)."
        }
        return null
    }
}
