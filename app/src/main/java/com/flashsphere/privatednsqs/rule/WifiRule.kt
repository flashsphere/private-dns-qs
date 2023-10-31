package com.flashsphere.privatednsqs.rule

sealed class WifiRule(
    ssid: String,
    wifiState: WifiState,
    dnsState: DnsState,
)

enum class WifiState {
    Connected,
    Disconnected
}

enum class DnsState {
    Off,
    Auto,
    On
}