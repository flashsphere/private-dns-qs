package com.flashsphere.privatednsqs.service

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.activity.MainActivity
import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.json.json
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class PrivateDnsTileService : TileService() {
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var privateDns: PrivateDns
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var tileInfoUpdater: TileInfoUpdater
    private lateinit var dnsConfigsFlow: Flow<List<DnsConfiguration>>

    override fun onCreate() {
        super.onCreate()

        settingsRepository = SettingsRepository(dataStore, json)
        dnsConfigsFlow = settingsRepository.getDnsConfigurationsFlow()
            .buffer(0)
            .shareIn(
                scope = mainScope,
                started = SharingStarted.Eagerly,
                replay = 1,
            )
        tileInfoUpdater = when {
            SamsungTileInfoUpdater.isApplicable() -> SamsungTileInfoUpdater(this, mainScope)
            else -> DefaultTileInfoUpdater(this)
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

        mainScope.launch {
            tileInfoUpdater.update(tile, privateDns.getCurrentDnsConfig(dnsConfigsFlow.first()))
        }
    }

    override fun onClick() {
        val isLocked = this.isSecure && this.isLocked

        mainScope.launch {
            val requireUnlock = settingsRepository.getRequireUnlock()

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
        if (!privateDns.hasPermission()) {
            showSnackbarMessage(NoPermissionMessage)
            return
        }

        val nextConfig = privateDns.getNextDnsConfig(dnsConfigsFlow.first()) ?: return

        privateDns.setDnsConfig(nextConfig)

        val tile = this.qsTile ?: return
        tileInfoUpdater.change(tile, nextConfig)
    }

    private fun showSnackbarMessage(snackbarMessage: SnackbarMessage) {
        val intent = MainActivity.getIntent(this, snackbarMessage)
        val pendingIntent = PendingIntentActivityWrapper(this, R.id.start_main_activity_request_code,
            intent, FLAG_UPDATE_CURRENT, false)
        TileServiceCompat.startActivityAndCollapse(this, pendingIntent)
    }
}