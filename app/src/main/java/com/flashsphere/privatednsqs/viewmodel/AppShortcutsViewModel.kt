package com.flashsphere.privatednsqs.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.shortcut.ShortcutManager
import com.flashsphere.privatednsqs.shortcut.ShortcutManager.Companion.MAX_SHORTCUTS
import com.flashsphere.privatednsqs.util.LauncherIconManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppShortcutsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val launcherIconManager: LauncherIconManager,
    private val shortcutManager: ShortcutManager,
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
        get() = shortcutManager.pinShortcutSupported

    val shortcutCountWarningFlow: StateFlow<String>
        field = MutableStateFlow("")

    init {
        combine(
            launcherIconVisibleStateFlow,
            settingsRepository.getDnsOffShortcutFlow(),
            settingsRepository.getDnsAutoShortcutFlow(),
            settingsRepository.getDnsToggleShortcutFlow(),
            settingsRepository.getDnsProvidersFlow(),
        ) { launcherVisible, dnsOffShortcut, dnsAutoShortcut, dnsToggleShortcut, dnsProviders ->
            shortcutManager.updateShortcuts(
                dnsProviders = dnsProviders,
                showOff = dnsOffShortcut,
                showAuto = dnsAutoShortcut,
                showToggle = dnsToggleShortcut,
            )

            val shortcutCount = shortcutManager.countShortcuts(
                dnsProviders = dnsProviders,
                showOff = dnsOffShortcut,
                showAuto = dnsAutoShortcut,
                showToggle = dnsToggleShortcut
            )

            if (!launcherVisible) {
                shortcutCountWarningFlow.value = context.getString(R.string.launcher_icon_no_app_shortcuts)
            } else if (shortcutCount > MAX_SHORTCUTS) {
                shortcutCountWarningFlow.value = context.getString(R.string.shortcut_limit_warning,
                    MAX_SHORTCUTS, shortcutCount)
            } else {
                shortcutCountWarningFlow.value = ""
            }
        }.launchIn(viewModelScope)
    }

    fun updateDnsOffShortcut(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsOffShortcut(checked) }
    }

    fun pinDnsOffShortcut() {
        shortcutManager.pinDnsOffShortcut()
    }

    fun updateDnsAutoShortcut(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsAutoShortcut(checked) }
    }

    fun pinDnsAutoShortcut() {
        shortcutManager.pinDnsAutoShortcut()
    }

    fun updateDnsToggleShortcut(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsToggleShortcut(checked) }
    }

    fun pinDnsToggleShortcut() {
        shortcutManager.pinDnsToggleShortcut()
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
            shortcutManager.pinShortcut(provider)
        }
    }

    fun toggleLauncherIcon() {
        launcherIconManager.toggleLauncherIcon()
    }
}
