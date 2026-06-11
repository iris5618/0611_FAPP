package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vessel_flights")
data class VesselFlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val flightNumber: String,
    val vesselName: String,
    val shipClass: String, // "A", "B", "C"
    val departureTime: String,
    val passengerCount: Int,
    val bookingStatus: String // "Active", "Refunded", "Rescheduled"
)
