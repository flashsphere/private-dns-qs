package com.flashsphere.privatednsqs.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.R
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherMenuItem(
    stateFlow: StateFlow<Boolean>,
    onClick: () -> Unit,
) {
    val showIcon by stateFlow.collectAsStateWithLifecycle()

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

    Tooltip(
        state = rememberTooltipState(),
        text = label,
    ) {
        IconButton(
            onClick = onClick,
        ) {
            Icon(painter = painterResource(iconRes),
                contentDescription = label)
        }
    }
}
