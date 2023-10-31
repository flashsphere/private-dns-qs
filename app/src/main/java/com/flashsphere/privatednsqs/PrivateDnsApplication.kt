package com.flashsphere.privatednsqs

import android.app.Application
import com.flashsphere.privatednsqs.util.NotificationChannelHelper
import com.google.android.material.color.DynamicColors
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant

class PrivateDnsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            plant(DebugTree())
        }
        NotificationChannelHelper(this).setupNotificationChannels()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}