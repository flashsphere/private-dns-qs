package com.flashsphere.privatednsqs.shortcut

import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.shortcut.ShortcutIdBuilder.Companion.DNS_AUTO_SHORTCUT_ID
import com.flashsphere.privatednsqs.shortcut.ShortcutIdBuilder.Companion.DNS_OFF_SHORTCUT_ID
import com.flashsphere.privatednsqs.shortcut.ShortcutIdBuilder.Companion.DNS_PROVIDER_SHORTCUT_ID_PREFIX
import com.flashsphere.privatednsqs.shortcut.ShortcutIdBuilder.Companion.DNS_TOGGLE_SHORTCUT_ID

class ShortcutIdBuilderImpl : ShortcutIdBuilder {
    override fun buildDnsOffShortcutId(): String {
        return DNS_OFF_SHORTCUT_ID
    }

    override fun buildDnsOffPinnedShortcutId(): String {
        return DNS_OFF_PINNED_SHORTCUT_ID
    }

    override fun buildDnsAutoShortcutId(): String {
        return DNS_AUTO_SHORTCUT_ID
    }

    override fun buildDnsAutoPinnedShortcutId(): String {
        return DNS_AUTO_PINNED_SHORTCUT_ID
    }

    override fun buildDnsToggleShortcutId(): String {
        return DNS_TOGGLE_SHORTCUT_ID
    }

    override fun buildDnsTogglePinnedShortcutId(): String {
        return DNS_TOGGLE_PINNED_SHORTCUT_ID
    }

    override fun buildDnsProviderShortcutId(dnsProvider: DnsProvider): String {
        return "${DNS_PROVIDER_SHORTCUT_ID_PREFIX}${dnsProvider.id}"
    }

    override fun buildDnsProviderPinnedShortcutId(dnsProvider: DnsProvider): String {
        return "${DNS_PROVIDER_PINNED_SHORTCUT_ID_PREFIX}${dnsProvider.id}"
    }

    companion object {
        private const val DNS_OFF_PINNED_SHORTCUT_ID = "privatedns.shortcut.pin.off"
        private const val DNS_AUTO_PINNED_SHORTCUT_ID = "privatedns.shortcut.pin.auto"
        private const val DNS_TOGGLE_PINNED_SHORTCUT_ID = "privatedns.shortcut.pin.toggle"
        private const val DNS_PROVIDER_PINNED_SHORTCUT_ID_PREFIX = "privatedns.shortcut.pin.on."
    }
}