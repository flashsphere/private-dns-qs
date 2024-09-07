package com.flashsphere.privatednsqs

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER
import com.flashsphere.privatednsqs.SharedPreferencesHelper.Companion.SHARED_PREF_REQUIRE_UNLOCK
import com.flashsphere.privatednsqs.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_AUTO
import com.flashsphere.privatednsqs.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_OFF
import com.flashsphere.privatednsqs.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_ON
import com.flashsphere.privatednsqs.ui.ChangesSavedMessage
import com.flashsphere.privatednsqs.ui.NoDnsHostnameMessage
import com.flashsphere.privatednsqs.ui.NoPermissionMessage
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val application: PrivateDnsApplication,
) : ViewModel() {
    private val preferences = SharedPreferencesHelper(application)
    private val contentResolver = application.contentResolver

    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.SUSPEND
    )
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _dnsOffChecked = MutableStateFlow(preferences.getBoolean(SHARED_PREF_TOGGLE_OFF, true))
    val dnsOffChecked = _dnsOffChecked.asStateFlow()

    private val _dnsAutoChecked = MutableStateFlow(preferences.getBoolean(SHARED_PREF_TOGGLE_AUTO, true))
    val dnsAutoChecked = _dnsAutoChecked.asStateFlow()

    private val _dnsOnChecked = MutableStateFlow(preferences.getBoolean(SHARED_PREF_TOGGLE_ON, true))
    val dnsOnChecked = _dnsOnChecked.asStateFlow()

    private val _requireUnlockChecked = MutableStateFlow(preferences.getBoolean(SHARED_PREF_REQUIRE_UNLOCK, false))
    val requireUnlockChecked = _requireUnlockChecked.asStateFlow()

    val dnsHostnameTextFieldState = TextFieldState(Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER) ?: "")

    fun dnsOffChecked(checked: Boolean) {
        _dnsOffChecked.value = checked
        preferences.update(SHARED_PREF_TOGGLE_OFF, checked)
    }
    fun dnsAutoChecked(checked: Boolean) {
        _dnsAutoChecked.value = checked
        preferences.update(SHARED_PREF_TOGGLE_AUTO, checked)
    }
    fun dnsOnChecked(checked: Boolean) {
        _dnsOnChecked.value = checked
        preferences.update(SHARED_PREF_TOGGLE_ON, checked)
    }
    fun requireUnlockChecked(checked: Boolean) {
        _requireUnlockChecked.value = checked
        preferences.update(SHARED_PREF_REQUIRE_UNLOCK, checked)
    }
    fun showSnackbarMessage(message: SnackbarMessage) {
        viewModelScope.launch { _snackbarMessages.emit(message) }
    }
    fun hasPermission(): Boolean {
        return application.checkCallingOrSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_DENIED
    }
    fun save() {
        if (!hasPermission()) {
            viewModelScope.launch { _snackbarMessages.emit(NoPermissionMessage) }
            return
        }
        val dnsHostName = dnsHostnameTextFieldState.text.trim().toString()
        if (dnsHostName.isEmpty()) {
            viewModelScope.launch { _snackbarMessages.emit(NoDnsHostnameMessage) }
            return
        }
        Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER, dnsHostName)
        viewModelScope.launch { _snackbarMessages.emit(ChangesSavedMessage) }
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