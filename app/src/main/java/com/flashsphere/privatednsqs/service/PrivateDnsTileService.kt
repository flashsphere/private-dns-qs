package com.flashsphere.privatednsqs.service

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.activity.MainActivity
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.util.DnsConfiguration
import com.flashsphere.privatednsqs.util.PrivateDns
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
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

@AndroidEntryPoint
class PrivateDnsTileService : TileService() {
    private lateinit var mainScope: CoroutineScope
    @Inject lateinit var privateDns: PrivateDns
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var tileInfoUpdater: TileInfoUpdater
    private lateinit var dnsConfigsFlow: Flow<List<DnsConfiguration>>

    override fun onCreate() {
        if (application !is PrivateDnsApplication) {
            ProcessPhoenix.triggerServiceRebirth(this, javaClass)
            return
        }
        super.onCreate()

        mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

        dnsConfigsFlow = settingsRepository.getDnsConfigurationsFlow()
            .buffer(0)
            .shareIn(
                scope = mainScope,
                started = SharingStarted.Eagerly,
                replay = 1,
            )
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
        tileInfoUpdater.update(tile, nextConfig)
    }

    private fun showSnackbarMessage(snackbarMessage: SnackbarMessage) {
        val intent = MainActivity.getIntent(this, snackbarMessage)
        val pendingIntent = PendingIntentActivityWrapper(this, R.id.start_main_activity_request_code,
            intent, FLAG_UPDATE_CURRENT, false)
        TileServiceCompat.startActivityAndCollapse(this, pendingIntent)
    }
}