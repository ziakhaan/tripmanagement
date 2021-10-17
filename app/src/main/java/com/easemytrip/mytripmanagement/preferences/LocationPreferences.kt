package com.easemytrip.mytripmanagement.preferences

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import kotlin.collections.ArrayList


object LocationPreferences {

    /**
     * Writing string to shared preference
     */
    fun writePreference(context: Context, prefStr: String) {
        val sharedPref =
            context?.getSharedPreferences("LOCATION_PREF", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("trip_json", prefStr)
            apply()
        }
    }

    /**
     * Retrieving shared preferences
     */
    fun getPreference(context: Context): String? {
        val sharedPref =
            context?.getSharedPreferences("LOCATION_PREF", Context.MODE_PRIVATE) ?: return ""
        return sharedPref.getString("trip_json", "")
    }

    /**
     * Creating a Trip Ref object for storing in preferences,this object will be serialized as JSON
     */
    fun createTrip(context: Context): TripPref {
        val jsonToParse = getPreference(context);

        // This will fetch the existing trip reference objects that are stored in the shared preferences
        val locationPrefList: ArrayList<TripPref> = getTripList(context)

        // The trip id since it is unique is calculated manually, by taking the last element's trip and adding it by 1

        var tripID: Long

        if (locationPrefList != null && locationPrefList.size > 0) {
            // This is the calculation for getting the last element
            val tripRefObj = locationPrefList[locationPrefList.size - 1]
            tripID = tripRefObj.trip_Id + 1
        } else {
            tripID = 1
        }

        // The trip object is created and returned with empty end time, end time is calculated when the stop button is pressed in the mylocationservices
        val tripPref = TripPref(tripID, getDate(), "", arrayListOf<LocationPref>())
        return tripPref
    }

    /**
     * This will return the existing trips serialized as TripPref objects, if there are no elements an empty list is returned
     * as empty string will produce a null pointer when deserializing back to the object
     */
    fun getTripList(context: Context): ArrayList<TripPref> {
        val jsonToParse = getPreference(context)
        val locationPrefList: ArrayList<TripPref>
        if (jsonToParse == null || jsonToParse == "" || jsonToParse.isEmpty()) {
            locationPrefList = arrayListOf<TripPref>()
        } else {
            locationPrefList = Gson().fromJson(jsonToParse, getTripRefType())
        }
        return locationPrefList
    }

    /**
     * This will actually contain list of locations that are associated with each trip start/stop that are added in
     * the existing list. The existing list is de-serialized and then the present data is added and it is stored back
     * in the preferences
     */
    fun updateTrip(context: Context, tripPref: TripPref) {
        tripPref.let {
            var pref: String? = getPreference(context)
            var locationList: ArrayList<TripPref>
            if (pref == null || pref == "" || pref.isEmpty()) {
                locationList = arrayListOf<TripPref>()
            } else {
                locationList = Gson().fromJson(pref, getTripRefType())
            }
            locationList.add(it)
            val jsonToParse = Gson().toJson(locationList, getTripRefType())
            writePreference(context, jsonToParse)
        }
    }

    /**
     * Ths will return the trip data as a string
     */
    fun getPreferenceAsJson(context: Context): String? {
        var locationList: ArrayList<TripPref> = getTripList(context)
        return Gson().toJson(locationList, getTripRefType())
    }

    /**
     * This is the type token used to identify the type of the object that will be
     * passed a parameter while de-serializing
     */
    fun getTripRefType(): Type? {
        val typePref: Type = object : TypeToken<ArrayList<TripPref>>() {}.type
        return typePref
    }

    /**
     * returns the current date as formatted in the below format
     */
    fun getDate(): String {
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
            return formatter.format(Date())
        } catch (e: Exception) {
            Log.e("mDate", e.toString())
            return ""
        }
    }

}