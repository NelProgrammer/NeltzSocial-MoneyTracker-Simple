package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxi_fares")
data class TaxiFareEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val routeName: String,
    val farePerTrip: Double,
    val tripsPerDay: Int = 2,
    val workingDaysPerMonth: Int = 20,
    val monthlyTotal: Double,
    val date: Long
)
