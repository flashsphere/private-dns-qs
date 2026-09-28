package com.flashsphere.privatednsqs.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.activity.DnsAutoActivity
import com.flashsphere.privatednsqs.activity.DnsOffActivity
import com.flashsphere.privatednsqs.activity.DnsOnActivity
import com.flashsphere.privatednsqs.activity.DnsToggleActivity
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import java.io.File

@Singleton
class ShortcutHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileOperations: FileOperations,
    private val settingsRepository: SettingsRepository,
    private val launcherIconManager: LauncherIconManager,
) {
    val maxShortcuts = 4
    val pinShortcutSupported: Boolean
        get() = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    val shortcutCountWarningFlow: StateFlow<String>
        field = MutableStateFlow("")

    suspend fun monitorShortcuts() {
        combine(
            settingsRepository.getDnsOffShortcutFlow(),
            settingsRepository.getDnsAutoShortcutFlow(),
            settingsRepository.getDnsToggleShortcutFlow(),
            settingsRepository.getDnsProvidersFlow(),
        ) { dnsOffShortcut, dnsAutoShortcut, dnsToggleShortcut, dnsProviders ->
            updateShortcuts(
                dnsProviders = dnsProviders,
                showOff = dnsOffShortcut,
                showAuto = dnsAutoShortcut,
                showToggle = dnsToggleShortcut,
            )

            val shortcutCount = countShortcuts(
                dnsProviders = dnsProviders,
                showOff = dnsOffShortcut,
                showAuto = dnsAutoShortcut,
                showToggle = dnsToggleShortcut
            )
            if (shortcutCount > maxShortcuts) {
                shortcutCountWarningFlow.value = context.getString(R.string.shortcut_limit_warning,
                    maxShortcuts, shortcutCount)
            } else {
                shortcutCountWarningFlow.value = ""
            }
        }.collect()
    }

    suspend fun populateShortcuts() {
        updateShortcuts(
            dnsProviders = settingsRepository.getDnsProviders(),
            showOff = settingsRepository.getDnsOffShortcut(),
            showAuto = settingsRepository.getDnsAutoShortcut(),
            showToggle = settingsRepository.getDnsToggleShortcut(),
        )
    }

    private fun countShortcuts(
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

    private suspend fun updateShortcuts(
        dnsProviders: List<DnsProvider>,
        showOff: Boolean,
        showAuto: Boolean,
        showToggle: Boolean,
    ) {
        if (!launcherIconManager.iconVisibleFlow.value) return

        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        if (showOff) shortcuts.add(buildDnsOffShortcut())
        if (showAuto) shortcuts.add(buildDnsAutoShortcut())
        if (showToggle) shortcuts.add(buildDnsToggleShortcut())

        dnsProviders
            .asSequence()
            .filter { it.shortcutEnabled }
            .take((maxShortcuts - shortcuts.size).coerceAtLeast(0))
            .forEach { provider ->
                shortcuts.add(buildShortcut(provider))
            }

        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }.onFailure {
            Timber.e(it, "Error setting dynamic shortcuts")
        }
    }

    suspend fun pinShortcut(dnsProvider: DnsProvider) {
        if (!pinShortcutSupported) return
        pinShortcut(buildShortcut(dnsProvider))
    }

    fun pinDnsOffShortcut() {
        if (!pinShortcutSupported) return
        pinShortcut(buildDnsOffShortcut())
    }

    fun pinDnsAutoShortcut() {
        if (!pinShortcutSupported) return
        pinShortcut(buildDnsAutoShortcut())
    }

    fun pinDnsToggleShortcut() {
        if (!pinShortcutSupported) return
        pinShortcut(buildDnsToggleShortcut())
    }

    private fun pinShortcut(shortcut: ShortcutInfoCompat) {
        if (!pinShortcutSupported) return
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private fun buildDnsOffShortcut(): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, DNS_OFF_SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.dns_off))
            .setLongLabel(context.getString(R.string.dns_off))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_off))
            .setIntent(Intent(context, DnsOffActivity::class.java).apply {
                action = "privatedns.shortcut.off"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            .build()
    }

    private fun buildDnsAutoShortcut(): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, DNS_AUTO_SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.dns_auto))
            .setLongLabel(context.getString(R.string.dns_auto))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, DnsAutoActivity::class.java)
                .apply { action = "privatedns.shortcut.auto" })
            .build()
    }

    private fun buildDnsToggleShortcut(): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, DNS_TOGGLE_SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.dns_toggle))
            .setLongLabel(context.getString(R.string.dns_toggle))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_toggle))
            .setIntent(Intent(context, DnsToggleActivity::class.java)
                .apply { action = "privatedns.shortcut.toggle" })
            .build()
    }

    private fun buildShortcutId(dnsProvider: DnsProvider): String {
        return "${DNS_PROVIDER_SHORTCUT_ID_PREFIX}.${dnsProvider.id}"
    }

    private suspend fun buildShortcut(dnsProvider: DnsProvider): ShortcutInfoCompat {
        val label = dnsProvider.label.takeUnless { it.isNullOrBlank() } ?: dnsProvider.hostname
        val intent = Intent(context, DnsOnActivity::class.java)
            .apply {
                action = "privatedns.shortcut.on.${dnsProvider.id}"
                putExtra("hostname", dnsProvider.hostname)
            }

        val builder = ShortcutInfoCompat.Builder(context, buildShortcutId(dnsProvider))
            .setShortLabel(label)
            .setLongLabel(label)
            .setIntent(intent)

        val icon = dnsProvider.icon?.let { iconName ->
            fileOperations.toBitmap(File(context.iconsDir, iconName))?.let { bitmap ->
                IconCompat.createWithBitmap(bitmap)
            }
        } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher_on)

        builder.setIcon(icon)

        return builder.build()
    }

    fun disablePinnedShortcut(dnsProvider: DnsProvider) {
        disablePinnedShortcuts(listOf(dnsProvider))
    }

    fun disablePinnedShortcuts(dnsProviders: List<DnsProvider>) {
        val shortcutIds = dnsProviders.map { buildShortcutId(it) }
        ShortcutManagerCompat.disableShortcuts(context, shortcutIds, "")
    }

    companion object {
        private const val DNS_OFF_SHORTCUT_ID = "privatedns.shortcut.off"
        private const val DNS_AUTO_SHORTCUT_ID = "privatedns.shortcut.auto"
        private const val DNS_TOGGLE_SHORTCUT_ID = "privatedns.shortcut.toggle"
        private const val DNS_PROVIDER_SHORTCUT_ID_PREFIX = "privatedns.shortcut.on."
    }
}
