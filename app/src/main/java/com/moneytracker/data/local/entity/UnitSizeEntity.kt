package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_sizes")
data class UnitSizeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val name: String
)
