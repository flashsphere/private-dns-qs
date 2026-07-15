package com.flashsphere.privatednsqs.service

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import android.service.quicksettings.Tile
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.DnsConfiguration
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.TileInfo
import com.flashsphere.privatednsqs.util.TileInfoFactory
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

interface TileInfoUpdater {
    suspend fun update(tile: Tile, dnsConfiguration: DnsConfiguration)
}

class DefaultTileInfoUpdater(
    context: Context,
    fileOperations: FileOperations,
    private val settingsRepository: SettingsRepository,
) : TileInfoUpdater {
    private val tileInfoFactory = TileInfoFactory(context, fileOperations)

    internal suspend fun createTileInfo(dnsConfiguration: DnsConfiguration): TileInfo {
        return tileInfoFactory.create(
            dnsConfiguration = dnsConfiguration,
            showInTileTitle = settingsRepository.getShowInTileTitle(),
            dnsAutoAsInactiveTile = settingsRepository.getDnsAutoAsInactiveTile(),
        )
    }

    override suspend fun update(tile: Tile, dnsConfiguration: DnsConfiguration) {
        val tileInfo = createTileInfo(dnsConfiguration)
        update(tile, tileInfo)
    }

    internal fun update(tile: Tile, tileInfo: TileInfo) {
        Timber.d("Updating tile: %s", tileInfo)

        tile.state = tileInfo.state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = tileInfo.stateDescription
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            tile.label = tileInfo.label
        } else {
            tile.label = tileInfo.label
            tile.subtitle = tileInfo.subtitle
        }
        tile.icon = tileInfo.icon
        tile.contentDescription = tileInfo.contentDescription
        tile.updateTile()
    }
}

/**
 * Workaround icon not updating when switching from auto -> on / dns a -> dns b
 * It appears that Samsung devices on One UI 8.5 do not update the icon
 * when the tile state doesn't change.
 */
class SamsungTileInfoUpdater(
    private val defaultTileInfoUpdater: DefaultTileInfoUpdater
) : TileInfoUpdater {
    private var currentDnsConfig: DnsConfiguration? = null

    override suspend fun update(tile: Tile, dnsConfiguration: DnsConfiguration) {
        val tileInfo = defaultTileInfoUpdater.createTileInfo(dnsConfiguration)
        if (currentDnsConfig != null && currentDnsConfig != dnsConfiguration) {
            if (tile.state == tileInfo.state) {
                Timber.d("Inverting tile state")
                tile.state = invertState(tileInfo.state)
                tile.updateTile()
            }

            delay(50.milliseconds)
        }
        currentDnsConfig = dnsConfiguration
        defaultTileInfoUpdater.update(tile, tileInfo)
    }

    private fun invertState(state: Int): Int {
        return when (state) {
            Tile.STATE_INACTIVE -> Tile.STATE_ACTIVE
            Tile.STATE_ACTIVE -> Tile.STATE_INACTIVE
            else -> state
        }
    }

    companion object {
        private const val ONE_UI_8_5 = 80500

        fun isApplicable(): Boolean {
            return Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                    getOneUiVersion() >= ONE_UI_8_5
        }

        private fun getOneUiVersion(): Int {
            return runCatching { SystemProperties.getInt("ro.build.version.oneui", 0) }
                .onFailure { Timber.e(it) }
                .getOrDefault(0)
        }
    }
}
