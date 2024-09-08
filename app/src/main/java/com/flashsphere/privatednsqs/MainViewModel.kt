package com.flashsphere.privatednsqs

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.dnsAutoToggle
import com.flashsphere.privatednsqs.datastore.dnsOffToggle
import com.flashsphere.privatednsqs.datastore.dnsOnToggle
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
) : ViewModel() {
    private val dataStore = application.dataStore
    private val privateDns = PrivateDns(application)

    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.SUSPEND
    )
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _dnsOffChecked = MutableStateFlow(dataStore.dnsOffToggle())
    val dnsOffChecked = _dnsOffChecked.asStateFlow()

    private val _dnsAutoChecked = MutableStateFlow(dataStore.dnsAutoToggle())
    val dnsAutoChecked = _dnsAutoChecked.asStateFlow()

    private val _dnsOnChecked = MutableStateFlow(dataStore.dnsOnToggle())
    val dnsOnChecked = _dnsOnChecked.asStateFlow()

    private val _requireUnlockChecked = MutableStateFlow(dataStore.requireUnlock())
    val requireUnlockChecked = _requireUnlockChecked.asStateFlow()

    val dnsHostnameTextFieldState = TextFieldState(privateDns.getHostname() ?: "")

    fun dnsOffChecked(checked: Boolean) {
        _dnsOffChecked.value = checked
        dataStore.dnsOffToggle(checked)
    }
    fun dnsAutoChecked(checked: Boolean) {
        _dnsAutoChecked.value = checked
        dataStore.dnsAutoToggle(checked)
    }
    fun dnsOnChecked(checked: Boolean) {
        _dnsOnChecked.value = checked
        dataStore.dnsOnToggle(checked)
    }
    fun requireUnlockChecked(checked: Boolean) {
        _requireUnlockChecked.value = checked
        dataStore.requireUnlock(checked)
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
        showSnackbarMessage(ChangesSavedMessage)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as PrivateDnsApplication
                return MainViewModel(application) as T
            }
        }
    }
}