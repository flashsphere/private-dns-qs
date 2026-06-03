package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import com.flashsphere.privatednsqs.ui.theme.Monospace
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(
    openHelpDialogFlow: StateFlow<Boolean>,
    openHelpDialog: (Boolean) -> Unit,
    showMoreInfo: () -> Unit,
) {
    val openDialogState = openHelpDialogFlow.collectAsStateWithLifecycle().value
    if (openDialogState) {
        HelpDialog(
            onDismiss = { openHelpDialog(false) },
            showMoreInfo = showMoreInfo,
        )
    }
}

@Composable
private fun HelpDialog(
    onDismiss: () -> Unit,
    showMoreInfo: () -> Unit,
) {
    val context = LocalContext.current
    CustomAlertDialog(
        onDismissRequest = onDismiss,
        content = {
            Column {
                Text(text = stringResource(R.string.message_help),
                    style = AppTypography.bodyMedium)
                SelectionContainer {
                    Text(text = stringResource(R.string.message_help_adb, context.packageName),
                        fontFamily = Monospace,
                        style = AppTypography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        buttons = {
            TextButton(onClick = showMoreInfo) { Text(stringResource(R.string.more_details)) }
            Spacer(modifier = Modifier.weight(1F))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}


@Preview
@Composable
private fun HelpDialogPreview() {
    Surface(Modifier.fillMaxSize()) {
        HelpDialog(
            onDismiss = {},
            showMoreInfo = {},
        )
    }
}
