package com.flashsphere.privatednsqs.shortcut

import com.flashsphere.privatednsqs.datastore.DnsProvider

interface ShortcutIdBuilder {
    fun buildDnsOffShortcutId(): String
    fun buildDnsOffPinnedShortcutId(): String
    fun buildDnsAutoShortcutId(): String
    fun buildDnsAutoPinnedShortcutId(): String
    fun buildDnsToggleShortcutId(): String
    fun buildDnsTogglePinnedShortcutId(): String
    fun buildDnsProviderShortcutId(dnsProvider: DnsProvider): String
    fun buildDnsProviderPinnedShortcutId(dnsProvider: DnsProvider): String

    companion object {
        protected const val DNS_OFF_SHORTCUT_ID = "privatedns.shortcut.off"
        protected const val DNS_AUTO_SHORTCUT_ID = "privatedns.shortcut.auto"
        protected const val DNS_TOGGLE_SHORTCUT_ID = "privatedns.shortcut.toggle"
        protected const val DNS_PROVIDER_SHORTCUT_ID_PREFIX = "privatedns.shortcut.on."
    }
}