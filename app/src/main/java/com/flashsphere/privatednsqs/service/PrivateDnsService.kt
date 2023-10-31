package com.flashsphere.privatednsqs.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.flashsphere.privatednsqs.R
import timber.log.Timber

class PrivateDnsService : Service() {

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var ongoingNotificationChannel: String
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private lateinit var networkCallback: NetworkCallback

    private val localBinder: IBinder = LocalBinder()

    private var ssid: String? = null

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()

        val applicationContext = this.applicationContext
        notificationManager = NotificationManagerCompat.from(applicationContext)
        ongoingNotificationChannel = getString(R.string.channel_name_ongoing)
        connectivityManager = ContextCompat.getSystemService(applicationContext, ConnectivityManager::class.java)!!
        wifiManager = ContextCompat.getSystemService(applicationContext, WifiManager::class.java)!!

        networkCallback = createNetworkCallback()
    }

    private fun createNetworkCallback(): NetworkCallback {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return createNetworkCallbackApi31()
        } else {
            return object : NetworkCallback() {
                override fun onLost(network: Network) {
                    ssid?.let { handleWifiDisconnected(it) }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    Timber.d("onCapabilitiesChanged")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        handleOnCapabilitiesChangedApi29(networkCapabilities)
                    } else {
                        wifiManager.connectionInfo?.ssid?.let { ssid ->
                            handleWifiConnected(ssid.replace("\"", ""))
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createNetworkCallbackApi31(): NetworkCallback {
        return object : NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onLost(network: Network) {
                ssid?.let { handleWifiDisconnected(it) }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Timber.d("onCapabilitiesChanged")
                handleOnCapabilitiesChangedApi29(networkCapabilities)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleOnCapabilitiesChangedApi29(networkCapabilities: NetworkCapabilities) {
        val transportInfo = networkCapabilities.transportInfo
        if (transportInfo != null && transportInfo is WifiInfo) {
            transportInfo.ssid?.let { ssid ->
                handleWifiConnected(ssid.replace("\"", ""))
            }
        }
    }

    override fun onDestroy() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        startForegroundService(createNotification())
        registerNetworkCallback()
        return START_STICKY
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun startForegroundService(notification: Notification) {
        try {
            Timber.i("Moving service to foreground")
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType)
        } catch (e: Exception) {
            Timber.e(e, "Can't move service to foreground")
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, getString(R.string.channel_name_ongoing))
            .setShowWhen(false)
            .setSmallIcon(R.drawable.ic_dnsauto)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentTitle(getString(R.string.qt_default))
            .build()
    }

    private fun handleWifiDisconnected(ssid: String) {
        if (this.ssid == null) {
            return
        }
        Timber.d("%s disconnected", ssid)
        this.ssid = null
    }

    private fun handleWifiConnected(ssid: String) {
        if (this.ssid == ssid) {
            return
        }
        this.ssid = ssid.replace("\"", "")
        Timber.d("%s connected", this.ssid)
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@PrivateDnsService
    }

    companion object {
        val NOTIFICATION_ID = R.id.ongoing_notification_id

        fun start(context: Context) {
            val intent = Intent(context, PrivateDnsService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}