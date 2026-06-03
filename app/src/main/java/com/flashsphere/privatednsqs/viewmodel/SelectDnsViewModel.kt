package com.flashsphere.privatednsqs.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.datastore.DnsConfiguration
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.dnsConfigurationsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SelectDnsViewModel(
    application: PrivateDnsApplication,
) : ViewModel() {
    private val dataStore = application.dataStore
    private val privateDns = PrivateDns(application)

    val dnsConfigs = mutableStateListOf<DnsConfiguration>()

    init {
        dataStore.dnsConfigurationsFlow()
            .onEach {
                dnsConfigs.clear()
                dnsConfigs.addAll(it)
            }
            .launchIn(viewModelScope)
    }

    fun hasPermission(): Boolean {
        return privateDns.hasPermission()
    }

    fun getCurrentDnsConfig(): DnsConfiguration {
        return privateDns.getCurrentDnsConfig()
    }

    fun selectDns(dnsConfig: DnsConfiguration) {
        privateDns.setDnsConfig(dnsConfig)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as PrivateDnsApplication
                return SelectDnsViewModel(application) as T
            }
        }
    }
}