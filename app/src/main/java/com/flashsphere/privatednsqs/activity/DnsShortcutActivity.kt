package com.flashsphere.privatednsqs.activity

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.core.content.pm.ShortcutManagerCompat
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.json.json
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage

abstract class DnsShortcutActivity : BaseActivity() {
    protected lateinit var privateDns: PrivateDns
    protected lateinit var settingsRepository: SettingsRepository

    protected open val showToastAfterSet: Boolean = false

    abstract fun getDnsConfig(): DnsConfiguration?

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        privateDns = PrivateDns(this)
        settingsRepository = SettingsRepository(dataStore, json)
    }

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