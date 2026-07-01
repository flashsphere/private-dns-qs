package com.flashsphere.privatednsqs.activity

import androidx.annotation.CallSuper
import androidx.core.content.pm.ShortcutManagerCompat
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.util.DnsConfiguration
import com.flashsphere.privatednsqs.util.PrivateDns
import jakarta.inject.Inject

abstract class DnsShortcutActivity : BaseActivity() {
    @Inject lateinit var privateDns: PrivateDns
    @Inject lateinit var settingsRepository: SettingsRepository

    protected open val showToastAfterSet: Boolean = false

    abstract fun getDnsConfig(): DnsConfiguration?

    @CallSuper
    override fun onStart() {
        super.onStart()
        reportShortcutUsed()
        if (privateDns.hasPermission()) {
            getDnsConfig()?.let {
                privateDns.setDnsConfig(it)
                if (showToastAfterSet) showToast(it)
            }
        } else {
            showMessage(NoPermissionMessage)
        }
        finish()
    }

    private fun showToast(dnsConfig: DnsConfiguration) {
        val application = this.application
        if (application !is PrivateDnsApplication) return

        val message = if (dnsConfig is DnsConfiguration.On) {
            dnsConfig.hostname
        } else {
            getString(dnsConfig.mode.labelResId)
        }
        application.showToast(message)
    }

    protected fun showMessage(message: SnackbarMessage) {
        MainActivity.startActivity(this, message)
    }

    protected fun reportShortcutUsed() {
        when (val shortcutId = intent?.action) {
            "privatedns.shortcut.toggle",
            "privatedns.shortcut.off",
            "privatedns.shortcut.auto",
            "privatedns.shortcut.on" -> {
                ShortcutManagerCompat.reportShortcutUsed(this, shortcutId)
            }
            else -> {}
        }
    }
}