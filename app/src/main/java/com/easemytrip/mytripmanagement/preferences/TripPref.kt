package com.easemytrip.mytripmanagement.preferences

/**
 * A data class for storing the location data as per provided format
 */
data class TripPref(
    val trip_Id: Long = 0,
    val start_time: String,
    var end_time: String,
    var locations: ArrayList<LocationPref>
)
