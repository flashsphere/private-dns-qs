package com.flashsphere.privatednsqs.util

import androidx.compose.runtime.Composable
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class LauncherIconManager @Inject constructor() {
    fun getShowIconFlow(scope: CoroutineScope): StateFlow<Boolean> = MutableStateFlow(true)

    @Composable
    fun LauncherMenuItem(onDismiss: () -> Unit) {
        // No-op
    }
}
