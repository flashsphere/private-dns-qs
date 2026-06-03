package com.flashsphere.privatednsqs.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.datastore.IdGenerator
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.dnsAutoToggle
import com.flashsphere.privatednsqs.datastore.dnsOffToggle
import com.flashsphere.privatednsqs.datastore.dnsProviders
import com.flashsphere.privatednsqs.datastore.dnsProvidersFlow
import com.flashsphere.privatednsqs.datastore.getStateFlow
import com.flashsphere.privatednsqs.datastore.requireUnlock
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(
    application: PrivateDnsApplication,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val dataStore = application.dataStore
    private val privateDns = PrivateDns(application)

    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.SUSPEND
    )
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _openHelpDialogFlow = savedStateHandle.getMutableStateFlow("open_help_menu", false)
    val openHelpDialogFlow = _openHelpDialogFlow.asStateFlow()

    val dnsOffChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_OFF_TOGGLE)
    val dnsAutoChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_AUTO_TOGGLE)
    val dnsProviders = mutableStateListOf<DnsProvider>()
    val requireUnlockChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.REQUIRE_UNLOCK)

    init {
        dataStore.dnsProvidersFlow()
            .onEach { list ->
                Timber.d("dns providers: %s", list)
                dnsProviders.clear()
                dnsProviders.addAll(list)
            }
            .launchIn(viewModelScope)

        _openHelpDialogFlow.value = !hasPermission()
    }

    fun openHelpDialog(open: Boolean) {
        _openHelpDialogFlow.value = open
    }

    fun dnsOffChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsOffToggle(checked) }
    }

    fun dnsAutoChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsAutoToggle(checked) }
    }

    fun requireUnlockChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.requireUnlock(checked) }
    }

    fun showSnackbarMessage(message: SnackbarMessage) {
        viewModelScope.launch {
            // wait until there's at least 1 subscriber before emitting
            _snackbarMessages.subscriptionCount.first { it > 0 }
            _snackbarMessages.emit(message)
        }
    }

    fun hasPermission(): Boolean {
        return privateDns.hasPermission()
    }

    fun validateDnsProvider(hostname: String): Boolean {
        val trimmedHost = hostname.trim()
        if (trimmedHost.isEmpty()) return true

        return !dnsProviders.any { it.hostname.equals(trimmedHost, true) }
    }

    fun addDnsProvider(hostname: String) {
        val trimmedHost = hostname.trim()
        if (trimmedHost.isEmpty()) return

        val providers = dnsProviders.toMutableList()
        viewModelScope.launch {
            providers.add(DnsProvider(id = IdGenerator.getNextId(dataStore), hostname = trimmedHost))
            Timber.d("Adding '%s'", trimmedHost)
            dataStore.dnsProviders(providers)
        }
    }

    fun updateDnsProvider(index: Int, newHostname: String) {
        val trimmedHost = newHostname.trim()
        if (trimmedHost.isEmpty()) return

        val providers = dnsProviders.toMutableList()
        if (index >= providers.size) return

        val provider = providers[index]
        providers[index] = provider.copy(hostname = trimmedHost)
        Timber.d("Updating '%s' to '%s", provider.hostname, trimmedHost)

        viewModelScope.launch {
            dataStore.dnsProviders(providers)
        }
    }

    fun deleteDnsProvider(index: Int) {
        val providers = dnsProviders.toMutableList()
        if (index >= providers.size) return

        val provider = providers.removeAt(index)
        Timber.d("Deleting '%s'", provider.hostname)

        viewModelScope.launch {
            dataStore.dnsProviders(providers)
        }
    }

    fun toggleDnsProvider(index: Int, enabled: Boolean) {
        val providers = dnsProviders.toMutableList()
        if (index >= providers.size) return

        val provider = providers[index]
        providers[index] = provider.copy(enabled = enabled)
        Timber.d("Toggling '%s' to %s", provider.hostname, enabled)

        viewModelScope.launch {
            dataStore.dnsProviders(providers)
        }
    }

    fun reorderDnsProvider(fromIndex: Int, toIndex: Int) {
        val fromItem = dnsProviders[fromIndex]
        dnsProviders[fromIndex] = dnsProviders[toIndex]
        dnsProviders[toIndex] = fromItem
    }

    fun reorderDnsProviders() {
        viewModelScope.launch { dataStore.dnsProviders(dnsProviders.toList()) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as PrivateDnsApplication
                val savedStateHandle = extras.createSavedStateHandle()
                return MainViewModel(application, savedStateHandle) as T
            }
        }
    }
}