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
        return buildDnsOffShortcutId()
    }

    override fun buildDnsAutoShortcutId(): String {
        return DNS_AUTO_SHORTCUT_ID
    }

    override fun buildDnsAutoPinnedShortcutId(): String {
        return buildDnsAutoShortcutId()
    }

    override fun buildDnsToggleShortcutId(): String {
        return DNS_TOGGLE_SHORTCUT_ID
    }

    override fun buildDnsTogglePinnedShortcutId(): String {
        return buildDnsToggleShortcutId()
    }

    override fun buildDnsProviderShortcutId(dnsProvider: DnsProvider): String {
        return "${DNS_PROVIDER_SHORTCUT_ID_PREFIX}${dnsProvider.id}"
    }

    override fun buildDnsProviderPinnedShortcutId(dnsProvider: DnsProvider): String {
        return buildDnsProviderShortcutId(dnsProvider)
    }
}