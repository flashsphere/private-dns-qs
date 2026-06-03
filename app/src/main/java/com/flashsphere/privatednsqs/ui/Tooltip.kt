package com.flashsphere.privatednsqs.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import com.flashsphere.privatednsqs.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    state: TooltipState = rememberTooltipState(),
    text: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip { Text(text = text, style = AppTypography.bodyMedium) }
        },
        state = state,
    ) {
        content()
    }
}
