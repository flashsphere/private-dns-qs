package com.jpwolfso.privdnsqt

import android.app.Application
import com.google.android.material.color.DynamicColors

class PrivateDnsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}