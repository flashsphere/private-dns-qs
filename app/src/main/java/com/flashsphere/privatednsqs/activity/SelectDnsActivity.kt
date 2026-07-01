package com.flashsphere.privatednsqs.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SelectDnsDialog
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.util.DnsConfiguration
import com.flashsphere.privatednsqs.viewmodel.SelectDnsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class SelectDnsActivity : BaseActivity() {
    private val viewModel: SelectDnsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!viewModel.hasPermission()) {
            openApp(NoPermissionMessage)
            return
        }

        setContent {
            SelectDnsDialog(
                configs = viewModel.dnsConfigs,
                currentConfig = viewModel.getCurrentDnsConfig(),
                onSelect = this::selectDns,
                openApp = this::openApp,
                onDismiss = this::finish,
            )
        }
    }

    private fun selectDns(dnsConfig: DnsConfiguration) {
        viewModel.selectDns(dnsConfig)
        lifecycleScope.launch {
            delay(100.milliseconds)
            finish()
        }
    }

    private fun openApp(message: SnackbarMessage? = null) {
        MainActivity.startActivity(this, message)
        finish()
    }
}