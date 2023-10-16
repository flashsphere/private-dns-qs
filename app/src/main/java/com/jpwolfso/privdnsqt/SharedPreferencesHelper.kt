package com.jpwolfso.privdnsqt

import android.app.Activity
import android.content.Context

class SharedPreferencesHelper(
        context: Context,
) {
    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE)

    fun update(key: String, value: Boolean) {
        sharedPreferences.edit()
                .putBoolean(key, value)
                .apply()
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    companion object {
        const val SHARED_PREFERENCES_NAME = "togglestates"

        const val SHARED_PREF_FIRST_RUN = "first_run"
        const val SHARED_PREF_TOGGLE_OFF = "toggle_off"
        const val SHARED_PREF_TOGGLE_AUTO = "toggle_auto"
        const val SHARED_PREF_TOGGLE_ON = "toggle_on"
    }
}