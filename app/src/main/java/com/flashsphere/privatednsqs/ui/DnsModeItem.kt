package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
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
    label: String,
) {
    val checked = state.collectAsStateWithLifecycle().value
    val checkboxInteractionSource = remember { MutableInteractionSource() }
    Row(modifier = Modifier
        .padding(horizontal = 4.dp)
        .focusProperties { canFocus = false }
        .clickable(
            interactionSource = checkboxInteractionSource,
            indication = null,
            onClick = { onClick(!checked) },
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onClick, interactionSource = checkboxInteractionSource)
        Text(modifier = Modifier.padding(end = 8.dp), text = label, style = AppTypography.bodyMedium)
    }
}