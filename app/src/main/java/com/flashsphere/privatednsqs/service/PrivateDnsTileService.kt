package com.flashsphere.privatednsqs.service

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.graphics.drawable.Icon
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
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var privateDns: PrivateDns

    override fun onCreate() {
        super.onCreate()
        privateDns = PrivateDns(this)
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()

        val tile = this.qsTile ?: return

        val dnsMode = privateDns.getDnsMode()
        when (dnsMode) {
            DnsMode.Off -> changeTileState(tile, Tile.STATE_INACTIVE, getString(dnsMode.labelResId), dnsMode.iconResId)
            DnsMode.Auto -> changeTileState(tile, Tile.STATE_ACTIVE, getString(dnsMode.labelResId), dnsMode.iconResId)
            DnsMode.On -> {
                val hostname = privateDns.getHostname()
                changeTileState(tile, Tile.STATE_ACTIVE, hostname ?: getString(dnsMode.labelResId), dnsMode.iconResId)
            }
        }
    }

    override fun onClick() {
        val isLocked = this.isSecure && this.isLocked

        coroutineScope.launch {
            val requireUnlock = dataStore.requireUnlock()

            if (!isLocked || !requireUnlock) {
                toggle()
            } else {
                unlockAndRun {
                    coroutineScope.launch {
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

        val nextDnsMode = privateDns.getNextDnsMode()
        when (nextDnsMode) {
            DnsMode.Off -> {
                privateDns.setDnsMode(nextDnsMode)
                changeTileState(tile, Tile.STATE_INACTIVE, getString(nextDnsMode.labelResId), nextDnsMode.iconResId)
            }
            DnsMode.Auto -> {
                privateDns.setDnsMode(nextDnsMode)
                changeTileState(tile, Tile.STATE_ACTIVE, getString(nextDnsMode.labelResId), nextDnsMode.iconResId)
            }
            DnsMode.On -> {
                val hostname = privateDns.getHostname()
                if (!hostname.isNullOrEmpty()) {
                    privateDns.setDnsMode(nextDnsMode)
                    changeTileState(tile, Tile.STATE_ACTIVE, hostname, nextDnsMode.iconResId)
                } else {
                    showSnackbarMessage(NoDnsHostnameMessage)
                }
            }
        }
    }

    private fun changeTileState(tile: Tile, state: Int, label: String, icon: Int) {
        tile.state = state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = getString(R.string.tile_name)
            tile.subtitle = label
        } else {
            tile.label = label
        }
        tile.icon = Icon.createWithResource(this, icon)
        tile.updateTile()
    }

    private fun showSnackbarMessage(snackbarMessage: SnackbarMessage) {
        val intent = MainActivity.getIntent(this, snackbarMessage)
        val pendingIntent = PendingIntentActivityWrapper(this, R.id.start_main_activity_request_code,
            intent, FLAG_UPDATE_CURRENT, false)
        TileServiceCompat.startActivityAndCollapse(this, pendingIntent)
    }
}