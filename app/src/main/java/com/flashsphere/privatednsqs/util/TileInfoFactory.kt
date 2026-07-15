package com.flashsphere.privatednsqs.util

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.core.graphics.drawable.toIcon
import com.flashsphere.privatednsqs.R
import java.io.File

class TileInfoFactory(
    private val context: Context,
    private val fileOperations: FileOperations,
) {
    suspend fun create(
        dnsConfiguration: DnsConfiguration,
        showInTileTitle: Boolean,
        dnsAutoAsInactiveTile: Boolean,
    ): TileInfo {
        val dnsMode = dnsConfiguration.mode
        val displayLabel = if (dnsConfiguration is DnsConfiguration.On && dnsConfiguration.hostname.isNotBlank()) {
            dnsConfiguration.hostname
        } else {
            context.getString(dnsMode.labelResId)
        }

        val label: String
        val subtitle: String?
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || showInTileTitle) {
            label = displayLabel
            subtitle = null
        } else {
            label = context.getString(R.string.tile_name)
            subtitle = displayLabel
        }

        val state = if (dnsConfiguration is DnsConfiguration.Auto && dnsAutoAsInactiveTile) {
            Tile.STATE_INACTIVE
        } else {
            dnsMode.tileState
        }

        val icon = toIcon(dnsConfiguration)

        return TileInfo(
            label = label,
            subtitle = subtitle,
            state = state,
            stateDescription = displayLabel,
            icon = icon,
            contentDescription = context.getString(R.string.tile_name),
        )
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
