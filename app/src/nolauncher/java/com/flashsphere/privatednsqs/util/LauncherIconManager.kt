package com.flashsphere.privatednsqs.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

@Singleton
class LauncherIconManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val launcherComponentName = ComponentName(context, "com.flashsphere.privatednsqs.LauncherActivity")
    val iconVisibleFlow: StateFlow<Boolean>
        field = MutableStateFlow(
            context.packageManager.getComponentEnabledSetting(launcherComponentName) == COMPONENT_ENABLED_STATE_ENABLED
        )

    fun toggleLauncherIcon() {
        val updatedState = !iconVisibleFlow.value
        val componentState = if (updatedState) {
            COMPONENT_ENABLED_STATE_ENABLED
        } else {
            COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            launcherComponentName,
            componentState,
            PackageManager.DONT_KILL_APP
        )
        iconVisibleFlow.value = updatedState
        Timber.d("Launcher icon visibility updated: %b", updatedState)
    }
}
