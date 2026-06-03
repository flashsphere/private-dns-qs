package com.flashsphere.privatednsqs.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SelectDnsDialog
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.viewmodel.SelectDnsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SelectDnsActivity : BaseActivity() {
    private val viewModel: SelectDnsViewModel by viewModels { SelectDnsViewModel.Factory }

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
                onSelect = this::selectDns,
                openApp = this::openApp,
                onDismiss = this::finish,
            )
        }
    }

    private fun selectDns(dnsConfig: DnsConfiguration) {
        viewModel.selectDns(dnsConfig)
        lifecycleScope.launch {
            delay(100)
            finish()
        }
    }

    private fun openApp(message: SnackbarMessage? = null) {
        MainActivity.startActivity(this, message)
        finish()
    }
}