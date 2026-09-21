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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ShortcutHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileOperations: FileOperations,
) {
    suspend fun updateShortcuts(
        dnsProviders: List<DnsProvider>,
        showToggle: Boolean,
        showOff: Boolean,
        showAuto: Boolean
    ) {
        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        if (showToggle) {
            shortcuts.add(ShortcutInfoCompat.Builder(context, "privatedns.shortcut.toggle")
                .setShortLabel(context.getString(R.string.dns_toggle))
                .setLongLabel(context.getString(R.string.dns_toggle))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_toggle))
                .setIntent(Intent(context, DnsToggleActivity::class.java).apply { action = "privatedns.shortcut.toggle" })
                .build())
        }
        
        if (showOff) {
            shortcuts.add(ShortcutInfoCompat.Builder(context, "privatedns.shortcut.off")
                .setShortLabel(context.getString(R.string.dns_off))
                .setLongLabel(context.getString(R.string.dns_off))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_off))
                .setIntent(Intent(context, DnsOffActivity::class.java).apply { action = "privatedns.shortcut.off" })
                .build())
        }

        if (showAuto) {
            shortcuts.add(ShortcutInfoCompat.Builder(context, "privatedns.shortcut.auto")
                .setShortLabel(context.getString(R.string.dns_auto))
                .setLongLabel(context.getString(R.string.dns_auto))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(Intent(context, DnsAutoActivity::class.java).apply { action = "privatedns.shortcut.auto" })
                .build())
        }

        val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val providersWithShortcuts = dnsProviders.filter { it.enabled && it.shortcutEnabled }

        providersWithShortcuts.take(maxShortcuts - shortcuts.size).forEach { provider ->
            val label = provider.label.takeUnless { it.isNullOrBlank() } ?: provider.hostname
            val intent = Intent(context, DnsOnActivity::class.java).apply {
                action = "privatedns.shortcut.on.${provider.id}"
                putExtra("hostname", provider.hostname)
            }

            val builder = ShortcutInfoCompat.Builder(context, "privatedns.shortcut.on.${provider.id}")
                .setShortLabel(label)
                .setLongLabel(label)
                .setIntent(intent)
            
            val icon = provider.icon?.let { iconName ->
                fileOperations.toBitmap(File(context.iconsDir, iconName))?.let { bitmap ->
                    IconCompat.createWithBitmap(bitmap)
                }
            } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher_on)

            builder.setIcon(icon)

            shortcuts.add(builder.build())
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    suspend fun pinShortcut(hostname: String, label: String?, iconFile: File?) {
        val shortLabel = label.takeUnless { it.isNullOrBlank() } ?: hostname
        val intent = Intent(context, DnsOnActivity::class.java).apply {
            action = "privatedns.shortcut.on.${hostname}"
            putExtra("hostname", hostname)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val icon = iconFile?.let {
            fileOperations.toBitmap(it)?.let { bitmap ->
                IconCompat.createWithBitmap(bitmap)
            }
        } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher_on)

        val shortcut = ShortcutInfoCompat.Builder(context, "privatedns.shortcut.pin.${hostname}")
            .setShortLabel(shortLabel)
            .setLongLabel(shortLabel)
            .setIcon(icon)
            .setIntent(intent)
            .build()

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
    }

    fun pinOffShortcut() {
        val shortcut = ShortcutInfoCompat.Builder(context, "privatedns.shortcut.off.pin")
            .setShortLabel(context.getString(R.string.dns_off))
            .setLongLabel(context.getString(R.string.dns_off))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_off))
            .setIntent(Intent(context, DnsOffActivity::class.java).apply { 
                action = "privatedns.shortcut.off"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            .build()

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
    }

    fun pinAutoShortcut() {
        val shortcut = ShortcutInfoCompat.Builder(context, "privatedns.shortcut.auto.pin")
            .setShortLabel(context.getString(R.string.dns_auto))
            .setLongLabel(context.getString(R.string.dns_auto))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, DnsAutoActivity::class.java).apply { 
                action = "privatedns.shortcut.auto" 
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            .build()

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
    }

    fun pinToggleShortcut() {
        val shortcut = ShortcutInfoCompat.Builder(context, "privatedns.shortcut.toggle.pin")
            .setShortLabel(context.getString(R.string.dns_toggle))
            .setLongLabel(context.getString(R.string.dns_toggle))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_toggle))
            .setIntent(Intent(context, DnsToggleActivity::class.java).apply { 
                action = "privatedns.shortcut.toggle" 
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            .build()

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
    }
}