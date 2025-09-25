package com.flashsphere.privatednsqs.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.dnsAutoToggle
import com.flashsphere.privatednsqs.datastore.dnsOffToggle
import com.flashsphere.privatednsqs.datastore.dnsOnToggle
import com.flashsphere.privatednsqs.datastore.getStateFlow
import com.flashsphere.privatednsqs.datastore.requireUnlock
import com.flashsphere.privatednsqs.ui.ChangesSavedMessage
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    val dnsOnChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_ON_TOGGLE)
    val requireUnlockChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.REQUIRE_UNLOCK)

    private val _dnsHostname = MutableStateFlow(privateDns.getHostname() ?: "")
    val dnsHostname = _dnsHostname.asStateFlow()

    val dnsHostnameTextFieldState = TextFieldState(_dnsHostname.value)

    init {
        _openHelpDialogFlow.value = !hasPermission()
    }

    fun openHelpDialog(open: Boolean) {
        _openHelpDialogFlow.value = open
    }

    fun reloadDnsHostname() {
        val updatedHostname = privateDns.getHostname() ?: ""

        dnsHostnameTextFieldState.text.trim().toString().let {
            if (it != updatedHostname && it == _dnsHostname.value) {
                dnsHostnameTextFieldState.setTextAndPlaceCursorAtEnd(updatedHostname)
            }
        }
        _dnsHostname.value = updatedHostname
    }

    fun dnsOffChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsOffToggle(checked) }
    }
    fun dnsAutoChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsAutoToggle(checked) }
    }
    fun dnsOnChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsOnToggle(checked) }
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
    fun save() {
        if (!hasPermission()) {
            showSnackbarMessage(NoPermissionMessage)
            return
        }
        val dnsHostName = dnsHostnameTextFieldState.text.trim().toString()
        if (dnsHostName.isEmpty()) {
            showSnackbarMessage(NoDnsHostnameMessage)
            return
        }
        privateDns.setHostname(dnsHostName)
        _dnsHostname.value = dnsHostName
        showSnackbarMessage(ChangesSavedMessage)
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