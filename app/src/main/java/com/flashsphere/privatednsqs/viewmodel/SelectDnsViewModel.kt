package com.flashsphere.privatednsqs.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.DnsConfiguration
import com.flashsphere.privatednsqs.util.PrivateDns
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class SelectDnsViewModel @Inject constructor(
    private val privateDns: PrivateDns,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val dnsConfigs = mutableStateListOf<DnsConfiguration>()

    init {
        settingsRepository.getDnsConfigurationsFlow()
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
        return privateDns.getCurrentDnsConfig(dnsConfigs)
    }

    fun selectDns(dnsConfig: DnsConfiguration) {
        privateDns.setDnsConfig(dnsConfig)
    }
}