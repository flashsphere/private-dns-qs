package com.flashsphere.privatednsqs.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.activity.DnsAutoActivity
import com.flashsphere.privatednsqs.activity.DnsOffActivity
import com.flashsphere.privatednsqs.activity.DnsOnActivity
import com.flashsphere.privatednsqs.activity.DnsToggleActivity
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.iconsDir
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File

@Singleton
class ShortcutBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileOperations: FileOperations,
) {
    fun buildDnsOffShortcut(shortcutId: String): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(context.getString(R.string.dns_off))
            .setLongLabel(context.getString(R.string.dns_off))
            .setRank(1)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_off))
            .setIntent(Intent(context, DnsOffActivity::class.java).apply {
                action = "privatedns.shortcut.off"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            .build()
    }

    fun buildDnsAutoShortcut(shortcutId: String): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(context.getString(R.string.dns_auto))
            .setLongLabel(context.getString(R.string.dns_auto))
            .setRank(2)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, DnsAutoActivity::class.java)
                .apply { action = "privatedns.shortcut.auto" })
            .build()
    }

    fun buildDnsToggleShortcut(shortcutId: String): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(context.getString(R.string.dns_toggle))
            .setLongLabel(context.getString(R.string.dns_toggle))
            .setRank(3)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_toggle))
            .setIntent(Intent(context, DnsToggleActivity::class.java)
                .apply { action = "privatedns.shortcut.toggle" })
            .build()
    }

    suspend fun buildProviderShortcut(shortcutId: String, dnsProvider: DnsProvider): ShortcutInfoCompat {
        val label = dnsProvider.label.takeUnless { it.isNullOrBlank() } ?: dnsProvider.hostname
        val intent = Intent(context, DnsOnActivity::class.java)
            .apply {
                action = "privatedns.shortcut.on.${dnsProvider.id}"
                putExtra("hostname", dnsProvider.hostname)
            }

        val builder = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(label)
            .setLongLabel(label)
            .setRank(4)
            .setIntent(intent)

        val icon = dnsProvider.icon?.let { iconName ->
            fileOperations.toBitmap(File(context.iconsDir, iconName))?.let { bitmap ->
                IconCompat.createWithBitmap(bitmap)
            }
        } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher_on)

        builder.setIcon(icon)

        return builder.build()
    }
}