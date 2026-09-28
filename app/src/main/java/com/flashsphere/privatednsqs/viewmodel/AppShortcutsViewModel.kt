package com.flashsphere.privatednsqs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.LauncherIconManager
import com.flashsphere.privatednsqs.util.ShortcutHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppShortcutsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val launcherIconManager: LauncherIconManager,
    private val shortcutHelper: ShortcutHelper,
) : ViewModel() {
    val dnsOffShortcutStateFlow = settingsRepository.getStateFlow(
        viewModelScope, PreferenceKeys.DNS_OFF_SHORTCUT)
    val dnsAutoShortcutStateFlow = settingsRepository.getStateFlow(
        viewModelScope, PreferenceKeys.DNS_AUTO_SHORTCUT)
    val dnsToggleShortcutStateFlow = settingsRepository.getStateFlow(
        viewModelScope, PreferenceKeys.DNS_TOGGLE_SHORTCUT)

    val dnsProviders = settingsRepository.getDnsProvidersFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList())

    val launcherIconVisibleStateFlow
        get() = launcherIconManager.iconVisibleFlow
    val pinShortcutSupported
        get() = shortcutHelper.pinShortcutSupported

    val shortcutCountWarningFlow: StateFlow<String>
        get() = shortcutHelper.shortcutCountWarningFlow

    init {
        viewModelScope.launch {
            shortcutHelper.monitorShortcuts()
        }
    }

    fun updateDnsOffShortcut(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsOffShortcut(checked) }
    }

    fun pinDnsOffShortcut() {
        shortcutHelper.pinDnsOffShortcut()
    }

    fun updateDnsAutoShortcut(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsAutoShortcut(checked) }
    }

    fun pinDnsAutoShortcut() {
        shortcutHelper.pinDnsAutoShortcut()
    }

    fun updateDnsToggleShortcut(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsToggleShortcut(checked) }
    }

    fun pinDnsToggleShortcut() {
        shortcutHelper.pinDnsToggleShortcut()
    }

    fun updateDnsProviderShortcut(index: Int, checked: Boolean) {
        val providers = dnsProviders.value.toMutableList()
        viewModelScope.launch {
            val provider = providers[index]
            providers[index] = provider.copy(shortcutEnabled = checked)
            settingsRepository.updateDnsProviders(providers)
        }
    }

    fun pinDnsProviderShortcut(index: Int) {
        val provider = dnsProviders.value[index]
        viewModelScope.launch {
            shortcutHelper.pinShortcut(provider)
        }
    }

    fun toggleLauncherIcon() {
        launcherIconManager.toggleLauncherIcon()
    }
}
