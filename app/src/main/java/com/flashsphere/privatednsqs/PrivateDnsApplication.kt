package com.flashsphere.privatednsqs

import android.app.Application
import com.google.android.material.color.DynamicColors

class PrivateDnsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}