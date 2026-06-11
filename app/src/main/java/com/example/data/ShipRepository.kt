package com.example.data

import kotlinx.coroutines.flow.Flow

class ShipRepository(private val shipDao: ShipDao) {

    val allFlights: Flow<List<VesselFlight>> = shipDao.getAllFlights()

    suspend fun insertFlights(flights: List<VesselFlight>) {
        shipDao.insertFlights(flights)
    }

    suspend fun updateBookingStatus(id: Int, status: String) {
        shipDao.updateBookingStatus(id, status)
    }

    suspend fun initializeDefaultFlights() {
        // Simple seeding
        val defaults = listOf(
            VesselFlight(
                flightNumber = "TC-101",
                vesselName = "麗娜快輪 (10,000噸級雙體客滾輪)",
                shipClass = "A",
                departureTime = "09:30",
                passengerCount = 380,
                bookingStatus = "Active"
            ),
            VesselFlight(
                flightNumber = "TC-102",
                vesselName = "百麗雙體快速客輪",
                shipClass = "A",
                departureTime = "13:00",
                passengerCount = 210,
                bookingStatus = "Active"
            ),
            VesselFlight(
                flightNumber = "TC-103",
                vesselName = "台中之星號大型客輪",
                shipClass = "A",
                departureTime = "15:45",
                passengerCount = 420,
                bookingStatus = "Active"
            ),
            VesselFlight(
                flightNumber = "TC-201",
                vesselName = "台中探險家號 (雙體遊艇)",
                shipClass = "B",
                departureTime = "10:15",
                passengerCount = 45,
                bookingStatus = "Active"
            ),
            VesselFlight(
                flightNumber = "TC-202",
                vesselName = "海鷹二號娛樂漁船",
                shipClass = "B",
                departureTime = "14:00",
                passengerCount = 32,
                bookingStatus = "Active"
            ),
            VesselFlight(
                flightNumber = "TC-301",
                vesselName = "奧德賽無動力帆船",
                shipClass = "C",
                departureTime = "08:30",
                passengerCount = 12,
                bookingStatus = "Active"
            ),
            VesselFlight(
                flightNumber = "TC-302",
                vesselName = "台中港北堤獨木舟/SUP活動團",
                shipClass = "C",
                departureTime = "16:30",
                passengerCount = 18,
                bookingStatus = "Active"
            )
        )
        // Clean and Seed to make sure it is pristine on first run
        shipDao.deleteAllFlights()
        shipDao.insertFlights(defaults)
    }
}
