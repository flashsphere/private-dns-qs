package com.flashsphere.privatednsqs.util

sealed class DnsConfiguration {
    abstract val mode: DnsMode

    object Off : DnsConfiguration() {
        override val mode = DnsMode.Off
    }

    object Auto : DnsConfiguration() {
        override val mode = DnsMode.Auto
    }

    data class On(val hostname: String, val icon: String?) : DnsConfiguration() {
        override val mode = DnsMode.On
    }
}
