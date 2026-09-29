package com.flashsphere.privatednsqs.shortcut

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.LauncherIconManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import timber.log.Timber

@Singleton
class ShortcutManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val launcherIconManager: LauncherIconManager,
    private val shortcutIdBuilder: ShortcutIdBuilder,
    private val shortcutBuilder: ShortcutBuilder,
) {

    val pinShortcutSupported: Boolean
        get() = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    fun countShortcuts(
        dnsProviders: List<DnsProvider>,
        showOff: Boolean,
        showAuto: Boolean,
        showToggle: Boolean,
    ): Int {
        var count = 0

        if (showOff) count++
        if (showAuto) count++
        if (showToggle) count++

        count += dnsProviders.count { it.shortcutEnabled }

        return count
    }

    suspend fun updateShortcuts() {
        updateShortcuts(
            dnsProviders = settingsRepository.getDnsProviders(),
            showOff = settingsRepository.getDnsOffShortcut(),
            showAuto = settingsRepository.getDnsAutoShortcut(),
            showToggle = settingsRepository.getDnsToggleShortcut(),
        )
    }

    suspend fun updateShortcuts(
        dnsProviders: List<DnsProvider>,
        showOff: Boolean,
        showAuto: Boolean,
        showToggle: Boolean
    ) {
        val dynamicShortcuts = mutableListOf<ShortcutInfoCompat>()
        val pinnedShortcuts = mutableListOf<ShortcutInfoCompat>()

        val currentPinnedShortcuts = ShortcutManagerCompat.getShortcuts(context, FLAG_MATCH_PINNED)
            .asSequence()
            .map { it.id }
            .toMutableSet()
        Timber.d("Current pinned shortcut ids = %s", currentPinnedShortcuts)

        processShortcut(
            shouldShowDynamicShortcut = showOff,
            dynamicShortcutId = shortcutIdBuilder.buildDnsOffShortcutId(),
            pinnedShortcutId = shortcutIdBuilder.buildDnsOffPinnedShortcutId(),
            currentPinnedShortcutIds = currentPinnedShortcuts,
            dynamicShortcuts = dynamicShortcuts,
            pinnedShortcuts = pinnedShortcuts,
            buildShortcut = { id -> shortcutBuilder.buildDnsOffShortcut(id) }
        )
        processShortcut(
            shouldShowDynamicShortcut = showAuto,
            dynamicShortcutId = shortcutIdBuilder.buildDnsAutoShortcutId(),
            pinnedShortcutId = shortcutIdBuilder.buildDnsAutoPinnedShortcutId(),
            currentPinnedShortcutIds = currentPinnedShortcuts,
            dynamicShortcuts = dynamicShortcuts,
            pinnedShortcuts = pinnedShortcuts,
            buildShortcut = { id -> shortcutBuilder.buildDnsAutoShortcut(id) }
        )
        processShortcut(
            shouldShowDynamicShortcut = showToggle,
            dynamicShortcutId = shortcutIdBuilder.buildDnsToggleShortcutId(),
            pinnedShortcutId = shortcutIdBuilder.buildDnsTogglePinnedShortcutId(),
            currentPinnedShortcutIds = currentPinnedShortcuts,
            dynamicShortcuts = dynamicShortcuts,
            pinnedShortcuts = pinnedShortcuts,
            buildShortcut = { id -> shortcutBuilder.buildDnsToggleShortcut(id) }
        )

        dnsProviders.forEach { provider ->
            processShortcut(
                shouldShowDynamicShortcut = provider.shortcutEnabled,
                dynamicShortcutId = shortcutIdBuilder.buildDnsProviderShortcutId(provider),
                pinnedShortcutId = shortcutIdBuilder.buildDnsProviderPinnedShortcutId(provider),
                currentPinnedShortcutIds = currentPinnedShortcuts,
                dynamicShortcuts = dynamicShortcuts,
                pinnedShortcuts = pinnedShortcuts,
                buildShortcut = { id -> shortcutBuilder.buildProviderShortcut(id, provider) }
            )
        }

        if (launcherIconManager.iconVisibleFlow.value) {
            runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, dynamicShortcuts) }
        }
        // pinned shortcuts can be updated regardless of launcher icon visibility
        runCatching { ShortcutManagerCompat.updateShortcuts(context, pinnedShortcuts) }
        runCatching { ShortcutManagerCompat.disableShortcuts(context, currentPinnedShortcuts.toList(), "") }
    }

    private suspend fun processShortcut(
        currentPinnedShortcutIds: MutableSet<String>,
        dynamicShortcuts: MutableList<ShortcutInfoCompat>,
        pinnedShortcuts: MutableList<ShortcutInfoCompat>,
        shouldShowDynamicShortcut: Boolean,
        dynamicShortcutId: String,
        pinnedShortcutId: String,
        buildShortcut: suspend (shortcutId: String) -> ShortcutInfoCompat,
    ) {
        val dynamicShortcut = if (shouldShowDynamicShortcut && dynamicShortcuts.size < MAX_SHORTCUTS) {
            buildShortcut(dynamicShortcutId)
                .also { dynamicShortcuts += it }
        } else {
            null
        }

        // dynamic shortcuts can be pinned by user on the home screen by long pressing on launcher icon
        if (currentPinnedShortcutIds.remove(dynamicShortcutId)) {
            val pinnedShortcut = dynamicShortcut ?: buildShortcut(dynamicShortcutId)
            pinnedShortcuts += pinnedShortcut
        }

        if (currentPinnedShortcutIds.remove(pinnedShortcutId) && dynamicShortcutId != pinnedShortcutId) {
            pinnedShortcuts += buildShortcut(pinnedShortcutId)
        }
    }

    fun pinDnsOffShortcut() {
        requestPinShortcut(
            shortcutBuilder.buildDnsOffShortcut(shortcutIdBuilder.buildDnsOffPinnedShortcutId())
        )
    }

    fun pinDnsAutoShortcut() {
        requestPinShortcut(
            shortcutBuilder.buildDnsAutoShortcut(shortcutIdBuilder.buildDnsAutoPinnedShortcutId())
        )
    }

    fun pinDnsToggleShortcut() {
        requestPinShortcut(
            shortcutBuilder.buildDnsToggleShortcut(shortcutIdBuilder.buildDnsTogglePinnedShortcutId())
        )
    }

    suspend fun pinShortcut(dnsProvider: DnsProvider) {
        requestPinShortcut(
            shortcutBuilder.buildProviderShortcut(
                shortcutId = shortcutIdBuilder.buildDnsProviderPinnedShortcutId(dnsProvider),
                dnsProvider = dnsProvider,
            )
        )
    }

    suspend fun updateShortcut(dnsProvider: DnsProvider) {
        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        val dynamicShortcutId = shortcutIdBuilder.buildDnsProviderShortcutId(dnsProvider)
        val pinnedShortcutId = shortcutIdBuilder.buildDnsProviderPinnedShortcutId(dnsProvider)

        shortcuts += shortcutBuilder.buildProviderShortcut(
            shortcutId = dynamicShortcutId,
            dnsProvider = dnsProvider,
        )
        if (dynamicShortcutId != pinnedShortcutId) {
            shortcuts += shortcutBuilder.buildProviderShortcut(
                shortcutId = pinnedShortcutId,
                dnsProvider = dnsProvider,
            )
        }
        runCatching { ShortcutManagerCompat.updateShortcuts(context, shortcuts) }
    }

    private fun requestPinShortcut(shortcut: ShortcutInfoCompat) {
        runCatching { ShortcutManagerCompat.requestPinShortcut(context, shortcut, null) }
    }

    fun disableShortcut(dnsProvider: DnsProvider) {
        disableShortcuts(listOf(dnsProvider))
    }

    fun disableShortcuts(dnsProviders: List<DnsProvider>) {
        val dynamicShortcutIds = mutableListOf<String>()
        val pinnedShortcutIds = mutableListOf<String>()

        dnsProviders.forEach { provider ->
            dynamicShortcutIds += shortcutIdBuilder.buildDnsProviderShortcutId(provider)
            pinnedShortcutIds += shortcutIdBuilder.buildDnsProviderPinnedShortcutId(provider)
        }

        runCatching { ShortcutManagerCompat.removeDynamicShortcuts(context, dynamicShortcutIds) }
        runCatching { ShortcutManagerCompat.disableShortcuts(context, pinnedShortcutIds, "") }
    }

    companion object {
        const val MAX_SHORTCUTS = 4
    }
}
