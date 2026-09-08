package com.flashsphere.privatednsqs.backup

import kotlinx.serialization.Serializable

@Serializable
data class DnsProviderSnapshot(
    val hostname: String,
    val label: String? = null,
    val enabled: Boolean,
    val iconBase64: String? = null,
)
