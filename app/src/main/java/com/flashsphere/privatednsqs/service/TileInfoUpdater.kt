package com.flashsphere.privatednsqs.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemProperties
import android.service.quicksettings.Tile
import androidx.core.graphics.drawable.toIcon
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.iconsDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

interface TileInfoUpdater {
    suspend fun update(tile: Tile, dnsConfiguration: DnsConfiguration)

    suspend fun change(tile: Tile, dnsConfiguration: DnsConfiguration) {
        update(tile, dnsConfiguration)
    }
}

open class DefaultTileInfoUpdater(
    private val context: Context,
) : TileInfoUpdater {
    private val fileOperations = FileOperations()

    override suspend fun update(tile: Tile, dnsConfiguration: DnsConfiguration) {
        Timber.d("Updating tile")

        val dnsMode = dnsConfiguration.mode
        val label = if (dnsConfiguration is DnsConfiguration.On && dnsConfiguration.hostname.isNotBlank()) {
            dnsConfiguration.hostname
        } else {
            context.getString(dnsMode.labelResId)
        }

        tile.state = dnsMode.tileState
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = context.getString(dnsMode.tileStateDescription)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = context.getString(R.string.tile_name)
            tile.subtitle = label
        } else {
            tile.label = label
        }
        tile.icon = toIcon(dnsConfiguration)
        tile.contentDescription = context.getString(R.string.tile_name)
        tile.updateTile()
    }

    private suspend fun toIcon(dnsConfiguration: DnsConfiguration): Icon {
        if (dnsConfiguration is DnsConfiguration.On && !dnsConfiguration.icon.isNullOrBlank()) {
            val bitmap = fileOperations.toBitmap(File(context.iconsDir, dnsConfiguration.icon))
            if (bitmap != null) {
                return bitmap.toIcon()
            }
        }
        return Icon.createWithResource(context, dnsConfiguration.mode.iconResId)
    }
}

/**
 * Workaround icon not updating when switching from auto -> on.
 * It appears that Samsung devices on One UI 8.5 do not update the icon
 * when the tile state doesn't change.
 */
class SamsungTileInfoUpdater(
    context: Context,
    private val mainScope: CoroutineScope,
) : DefaultTileInfoUpdater(context) {

    override suspend fun change(tile: Tile, dnsConfiguration: DnsConfiguration) {
        val dnsMode = dnsConfiguration.mode
        if (tile.state == dnsMode.tileState) {
            Timber.d("Inverting tile state")
            tile.state = invertState(dnsMode.tileState)
            tile.updateTile()
        }
        mainScope.launch {
            delay(50.milliseconds)
            update(tile, dnsConfiguration)
        }
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
