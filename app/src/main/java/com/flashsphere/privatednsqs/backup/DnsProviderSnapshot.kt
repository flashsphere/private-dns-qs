package com.flashsphere.privatednsqs.backup

import com.flashsphere.privatednsqs.datastore.DnsProvider
import kotlinx.serialization.Serializable

@Serializable
data class DnsProviderSnapshot(
    val hostname: String,
    val enabled: Boolean,
) {
    constructor(dnsProvider: DnsProvider) : this(
        hostname = dnsProvider.hostname,
        enabled = dnsProvider.enabled,
    )

    fun toDnsProvider(id: Int): DnsProvider {
        return DnsProvider(
            id = id,
            hostname = hostname,
            enabled = enabled,
        )
    }
}
