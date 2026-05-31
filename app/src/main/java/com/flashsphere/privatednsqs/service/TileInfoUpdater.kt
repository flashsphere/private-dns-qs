package com.flashsphere.privatednsqs.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

interface TileInfoUpdater {
    fun updateTile(tile: Tile, dnsMode: DnsMode, customLabel: String? = null)
}

class DefaultTileInfoUpdater(
    private val context: Context,
) : TileInfoUpdater {
    override fun updateTile(tile: Tile, dnsMode: DnsMode, customLabel: String?) {
        Timber.d("Updating tile")
        val label = customLabel ?: context.getString(dnsMode.labelResId)

        tile.state = dnsMode.tileState
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = context.getString(R.string.tile_name)
            tile.subtitle = label
        } else {
            tile.label = label
        }
        tile.icon = Icon.createWithResource(context, dnsMode.iconResId)
        tile.updateTile()
    }
}

/**
 * Workaround icon not updating when switching from auto -> on.
 * It appears that Samsung devices do not update the icon when the tile state doesn't change.
 */
class SamsungTileInfoUpdater(
    context: Context,
    private val mainScope: CoroutineScope,
) : TileInfoUpdater {
    private val defaultTileStateChanger = DefaultTileInfoUpdater(context)

    override fun updateTile(tile: Tile, dnsMode: DnsMode, customLabel: String?) {
        if (tile.state == dnsMode.tileState) {
            Timber.d("Inverting tile state")
            tile.state = invertState(dnsMode.tileState)
            tile.updateTile()
        }
        mainScope.launch {
            delay(10.milliseconds)
            defaultTileStateChanger.updateTile(tile, dnsMode, customLabel)
        }
    }

    private fun invertState(state: Int): Int {
        return when (state) {
            Tile.STATE_INACTIVE -> Tile.STATE_ACTIVE
            Tile.STATE_ACTIVE -> Tile.STATE_INACTIVE
            else -> state
        }
    }
}
