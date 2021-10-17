package com.easemytrip.mytripmanagement.preferences

import android.content.Context


object AppPreferences {

    const val START_BTN_ENABLE: String = "start_btn_enable"
    const val STOP_BTN_ENABLE: String = "stop_btn_enable"

    /**
     * Writing string to shared preference
     */
    fun writeBooleanPreference(context: Context, prefName: String, isEnabled: Boolean) {
        val sharedPref = context?.getSharedPreferences("APP_PREF", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(prefName, isEnabled)
            apply()
        }
    }

    /**
     * Retrieving shared preferences
     */
    fun getPreference(context: Context, prefName: String): Boolean {
        val sharedPref =
            context?.getSharedPreferences("APP_PREF", Context.MODE_PRIVATE) ?: return true
        return sharedPref.getBoolean(prefName, true)
    }
}