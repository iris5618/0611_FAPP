package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShipDao {
    @Query("SELECT * FROM vessel_flights ORDER BY departureTime ASC")
    fun getAllFlights(): Flow<List<VesselFlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlights(flights: List<VesselFlight>)

    @Query("UPDATE vessel_flights SET bookingStatus = :status WHERE id = :id")
    suspend fun updateBookingStatus(id: Int, status: String)

    @Query("DELETE FROM vessel_flights")
    suspend fun deleteAllFlights()
}
