package com.flashsphere.privatednsqs.util

import android.graphics.drawable.Icon

data class TileInfo(
    val label: String,
    val subtitle: String?,
    val state: Int,
    val stateDescription: String,
    val icon: Icon,
    val contentDescription: String,
)
