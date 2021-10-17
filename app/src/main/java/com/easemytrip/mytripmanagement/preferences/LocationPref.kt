package com.easemytrip.mytripmanagement.preferences

/**
 * model data class for storing objects in Shared preferences
 */
data class LocationPref(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timeStamp: String
)
