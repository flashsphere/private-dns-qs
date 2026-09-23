package com.flashsphere.privatednsqs.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.R
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class LauncherIconManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val launcherComponentName = ComponentName(context, "com.flashsphere.privatednsqs.LauncherActivity")
    val showIconFlow: StateFlow<Boolean>
        field = MutableStateFlow(
            context.packageManager.getComponentEnabledSetting(launcherComponentName) == COMPONENT_ENABLED_STATE_ENABLED
        )

    @Composable
    fun LauncherMenuItem(onDismiss: () -> Unit) {
        val scope = rememberCoroutineScope()
        val showIcon by showIconFlow.collectAsStateWithLifecycle()
        
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
                    updateSystemState(newValue)
                    onDismiss()
                }
            }
        )
    }

    private fun updateSystemState(visible: Boolean) {
        val state = if (visible) {
            COMPONENT_ENABLED_STATE_ENABLED
        } else {
            COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            launcherComponentName,
            state,
            PackageManager.DONT_KILL_APP
        )
        showIconFlow.value = visible
        Timber.d("Launcher icon visibility updated: %b", visible)
    }
}
