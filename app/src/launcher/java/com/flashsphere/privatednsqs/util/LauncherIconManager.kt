package com.flashsphere.privatednsqs.util

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class LauncherIconManager @Inject constructor() {
    val iconVisibleFlow: StateFlow<Boolean>
        field = MutableStateFlow(true)

    fun toggleLauncherIcon() {
        // No-op
    }
}
