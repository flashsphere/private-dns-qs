package com.flashsphere.privatednsqs.service

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.activity.MainActivity
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.requireUnlock
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PrivateDnsTileService : TileService() {
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var privateDns: PrivateDns
    private lateinit var tileInfoUpdater: TileInfoUpdater

    override fun onCreate() {
        super.onCreate()
        tileInfoUpdater = if (!Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            DefaultTileInfoUpdater(this)
        } else {
            SamsungTileInfoUpdater(this, mainScope)
        }
        privateDns = PrivateDns(this)
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()

        val tile = this.qsTile ?: return

        when (val dnsMode = privateDns.getDnsMode()) {
            DnsMode.Off -> changeTileState(tile, dnsMode)
            DnsMode.Auto -> changeTileState(tile, dnsMode)
            DnsMode.On -> changeTileState(tile, dnsMode, privateDns.getHostname())
        }
    }

    override fun onClick() {
        val isLocked = this.isSecure && this.isLocked

        mainScope.launch {
            val requireUnlock = dataStore.requireUnlock()

            if (!isLocked || !requireUnlock) {
                toggle()
            } else {
                unlockAndRun {
                    mainScope.launch {
                        toggle()
                    }
                }
            }
        }
    }

    private suspend fun toggle() {
        val tile = this.qsTile ?: return

        if (!privateDns.hasPermission()) {
            showSnackbarMessage(NoPermissionMessage)
            return
        }

        when (val nextDnsMode = privateDns.getNextDnsMode()) {
            DnsMode.Off -> {
                privateDns.setDnsMode(nextDnsMode)
                changeTileState(tile, nextDnsMode)
            }
            DnsMode.Auto -> {
                privateDns.setDnsMode(nextDnsMode)
                changeTileState(tile, nextDnsMode)
            }
            DnsMode.On -> {
                val hostname = privateDns.getHostname()
                if (!hostname.isNullOrEmpty()) {
                    privateDns.setDnsMode(nextDnsMode)
                    changeTileState(tile, nextDnsMode, hostname)
                } else {
                    showSnackbarMessage(NoDnsHostnameMessage)
                }
            }
        }
    }

    private fun changeTileState(tile: Tile, dnsMode: DnsMode, hostname: String? = null) {
        tileInfoUpdater.updateTile(tile, dnsMode, hostname)
    }

    private fun showSnackbarMessage(snackbarMessage: SnackbarMessage) {
        val intent = MainActivity.getIntent(this, snackbarMessage)
        val pendingIntent = PendingIntentActivityWrapper(this, R.id.start_main_activity_request_code,
            intent, FLAG_UPDATE_CURRENT, false)
        TileServiceCompat.startActivityAndCollapse(this, pendingIntent)
    }
}