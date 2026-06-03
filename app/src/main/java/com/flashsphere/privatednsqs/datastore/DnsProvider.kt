package com.flashsphere.privatednsqs.datastore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class DnsProvider(
    val id: Int,
    val hostname: String,
    val enabled: Boolean = true,
) : Parcelable
