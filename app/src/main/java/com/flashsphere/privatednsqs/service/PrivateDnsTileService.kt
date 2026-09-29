package com.flashsphere.privatednsqs.service

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.service.quicksettings.Tile
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PrivateDnsTileService : TileService() {
    private lateinit var mainScope: CoroutineScope
    @Inject lateinit var privateDns: PrivateDns
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var tileInfoUpdater: TileInfoUpdater

    private val allDnsConfigsFlow = MutableSharedFlow<List<DnsConfiguration>>(replay = 1)
    private val enabledDnsConfigsFlow = MutableSharedFlow<List<DnsConfiguration>>(replay = 1)
    private var updateTileJob: Job? = null

    override fun onCreate() {
        if (application !is PrivateDnsApplication) {
            ProcessPhoenix.triggerServiceRebirth(this, javaClass)
            return
        }
        super.onCreate()

        mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

        combine(
            settingsRepository.getDnsOffToggleFlow(),
            settingsRepository.getDnsAutoToggleFlow(),
            settingsRepository.getDnsProvidersFlow(),
        ) { dnsOffToggle, dnsAutoToggle, dnsProviders ->
            val all = mutableListOf<DnsConfiguration>()
            val enabled = mutableListOf<DnsConfiguration>()

            all += DnsConfiguration.Off
            if (dnsOffToggle) {
                enabled += DnsConfiguration.Off
            }

            all += DnsConfiguration.Auto
            if (dnsAutoToggle) {
                enabled += DnsConfiguration.Auto
            }

            dnsProviders.forEach {
                val config = DnsConfiguration.On(it.hostname, it.label, it.icon)
                all += config
                if (it.enabled) {
                    enabled += config
                }
            }
            all to enabled
        }
        .onEach { (all, enabled) ->
            allDnsConfigsFlow.emit(all)
            enabledDnsConfigsFlow.emit(enabled)
        }
        .launchIn(mainScope)
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()

        val tile = this.qsTile ?: return

        mainScope.launch {
            updateTile(tile, privateDns.getCurrentDnsConfig(allDnsConfigsFlow.first()))
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

        val nextConfig = privateDns.getNextDnsConfig(enabledDnsConfigsFlow.first()) ?: return
        privateDns.setDnsConfig(nextConfig)

        val tile = this.qsTile ?: return
        updateTile(tile, nextConfig)
    }

    private fun updateTile(tile: Tile, dnsConfiguration: DnsConfiguration) {
        updateTileJob?.cancel()
        updateTileJob = mainScope.launch {
            tileInfoUpdater.update(tile, dnsConfiguration)
        }
    }

    private fun showSnackbarMessage(snackbarMessage: SnackbarMessage) {
        val intent = MainActivity.getIntent(this, snackbarMessage)
        val pendingIntent = PendingIntentActivityWrapper(this, R.id.start_main_activity_request_code,
            intent, FLAG_UPDATE_CURRENT, false)
        TileServiceCompat.startActivityAndCollapse(this, pendingIntent)
    }
}