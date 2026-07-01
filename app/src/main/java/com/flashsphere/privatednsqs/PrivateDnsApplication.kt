package com.flashsphere.privatednsqs

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composer
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class PrivateDnsApplication : Application(), SingletonImageLoader.Factory {
    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    @Inject lateinit var imageLoader: ImageLoader

    private var toast: Toast? = null

    override fun onCreate() {
        if (ProcessPhoenix.isPhoenixProcess(this)) {
            return
        }
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
        } else {
            Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.Auto)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return imageLoader
    }

    fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            .apply { show() }
    }
}