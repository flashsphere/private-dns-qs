package com.flashsphere.privatednsqs.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.PreferenceKey
import com.flashsphere.privatednsqs.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class LauncherIconManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private val SHOW_LAUNCHER_ICON = PreferenceKey(booleanPreferencesKey("show_launcher_icon"), false)
    }

    fun getShowIconFlow(scope: CoroutineScope): StateFlow<Boolean> =
        settingsRepository.getPreferenceStateFlow(scope, SHOW_LAUNCHER_ICON)

    @Composable
    fun LauncherMenuItem(onDismiss: () -> Unit) {
        val scope = rememberCoroutineScope()
        val showIcon by getShowIconFlow(scope).collectAsStateWithLifecycle()
        
        val label = if (showIcon) {
            stringResource(R.string.hide_launcher_icon)
        } else {
            stringResource(R.string.show_launcher_icon)
        }

        val iconRes = if (showIcon) {
            R.drawable.ic_dns_off
        } else {
            R.drawable.ic_dns_on
        }

        DropdownMenuItem(
            leadingIcon = { 
                Icon(
                    painter = painterResource(iconRes), 
                    contentDescription = null
                ) 
            },
            text = { Text(label) },
            onClick = {
                val newValue = !showIcon
                scope.launch {
                    settingsRepository.updatePreference(SHOW_LAUNCHER_ICON, newValue)
                    updateSystemState(newValue)
                    onDismiss()
                }
            }
        )
    }

    private fun updateSystemState(visible: Boolean) {
        val componentName = ComponentName(context, "com.flashsphere.privatednsqs.LauncherActivity")
        val state = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            componentName,
            state,
            PackageManager.DONT_KILL_APP
        )
        Timber.d("Launcher icon visibility updated: %b", visible)
    }
}
