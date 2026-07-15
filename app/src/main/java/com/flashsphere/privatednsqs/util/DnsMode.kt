package com.flashsphere.privatednsqs.util

import android.service.quicksettings.Tile
import com.flashsphere.privatednsqs.R

enum class DnsMode(
    val value: String,
    val iconResId: Int,
    val labelResId: Int,
    val tileState: Int,
) {
    Off(
        value = "off",
        iconResId = R.drawable.ic_dns_off,
        labelResId = R.string.off,
        tileState = Tile.STATE_INACTIVE,
    ),
    Auto(
        value = "opportunistic",
        iconResId = R.drawable.ic_dns_auto,
        labelResId = R.string.auto,
        tileState = Tile.STATE_ACTIVE,
    ),
    On(
        value = "hostname",
        iconResId = R.drawable.ic_dns_on,
        labelResId = R.string.on,
        tileState = Tile.STATE_ACTIVE,
    );
}
