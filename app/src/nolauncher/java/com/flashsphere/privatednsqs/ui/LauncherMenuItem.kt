package com.flashsphere.privatednsqs.ui

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.R
import kotlinx.coroutines.flow.StateFlow

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

    DropdownMenuItem(
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label
            )
        },
        text = { Text(label) },
        onClick = onClick
    )
}
