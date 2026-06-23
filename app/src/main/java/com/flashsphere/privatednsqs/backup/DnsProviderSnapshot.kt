package com.flashsphere.privatednsqs.backup

import kotlinx.serialization.Serializable

@Serializable
data class DnsProviderSnapshot(
    val hostname: String,
    val enabled: Boolean,
    val iconBase64: String? = null,
)
