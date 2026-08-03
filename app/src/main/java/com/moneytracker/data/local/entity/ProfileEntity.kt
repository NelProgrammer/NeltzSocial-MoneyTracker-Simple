package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val isGuest: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
