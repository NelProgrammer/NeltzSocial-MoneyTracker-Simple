package com.moneytracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class TransportMode(val displayName: String) {
    TAXI("Taxi"),
    UBER("Uber / Ride-hail"),
    BOLT("Bolt"),
    BUS("Bus"),
    TRAIN("Train"),
    METRO("Metro / Subway"),
    WALK("Walk / Last Mile"),
    OTHER("Other")
}

@Entity(tableName = "commute_journeys")
data class CommuteJourneyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 0,
    val journeyName: String,
    val isDefaultWorkday: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "commute_legs",
    foreignKeys = [
        ForeignKey(
            entity = CommuteJourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journeyId")]
)
data class CommuteLegEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val journeyId: Long = 0L,
    val legOrder: Int = 1,
    val legName: String,
    val mode: TransportMode = TransportMode.TAXI,
    val farePerTrip: Double = 0.0,
    val tripsPerDay: Int = 2,
    val workingDaysPerMonth: Int = 20,
    val monthlyBudget: Double = 0.0,
    val trafficDensity: String = "LOW", // LOW, MODERATE, HEAVY, SEVERE
    val estimatedDelayMinutes: Int = 0,
    val traverseTimeMinutes: Int = 15
)

data class JourneyWithLegs(
    @Embedded val journey: CommuteJourneyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "journeyId"
    )
    val legs: List<CommuteLegEntity>
) {
    val totalDailyCost: Double
        get() = legs.sumOf { it.farePerTrip * it.tripsPerDay }

    val totalMonthlyBudget: Double
        get() = legs.sumOf { it.monthlyBudget }

    val totalTraverseTimeMinutes: Int
        get() = legs.sumOf { it.traverseTimeMinutes + it.estimatedDelayMinutes }
}
