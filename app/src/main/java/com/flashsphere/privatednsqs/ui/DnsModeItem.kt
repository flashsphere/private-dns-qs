package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DnsModeItem(
    state: StateFlow<Boolean>,
    onClick: (checked: Boolean) -> Unit,
    onLabelClick: (() -> Unit)? = null,
    label: String,
    checkboxEnabled: Boolean = true,
) {
    val checked = state.collectAsStateWithLifecycle().value
    Row(modifier = Modifier
        .focusProperties { canFocus = false }
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = if (checkboxEnabled) checked else false,
                onCheckedChange = if (checkboxEnabled) onClick else null,
                enabled = checkboxEnabled
            )
        }
        Text(
            modifier = Modifier
                .weight(1F)
                .clickable(
                    enabled = onLabelClick != null,
                    onClick = { onLabelClick?.invoke() },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                .padding(vertical = 12.dp),
            text = label,
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
