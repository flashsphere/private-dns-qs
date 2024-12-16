package com.flashsphere.privatednsqs

import android.app.Application
import com.jakewharton.processphoenix.ProcessPhoenix
import timber.log.Timber

class PrivateDnsApplication : Application() {
    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun onCreate() {
        if (ProcessPhoenix.isPhoenixProcess(this)) {
            return
        }
        super.onCreate()
    }
}