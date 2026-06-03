package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.flashsphere.privatednsqs.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAlertDialog(
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            content()
        }
    }
}

@Composable
fun CustomAlertDialog(
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    onDismissRequest: () -> Unit,
    title: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
    buttons: @Composable RowScope.() -> Unit,
) {
    CustomAlertDialog(
        modifier = Modifier.width(IntrinsicSize.Max).then(modifier),
        properties = properties,
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.padding(DialogPadding)) {
            title?.let {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.merge(AppTypography.bodyLarge),
                ) {
                    Box(Modifier.padding(bottom = 16.dp)) {
                        it()
                    }
                }
            }
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.merge(AppTypography.bodyMedium),
            ) {
                Box(Modifier.fillMaxWidth().weight(weight = 1F, fill = false)) {
                    content()
                }
            }
            Row(
                modifier = Modifier.align(Alignment.End).fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                buttons()
            }
        }
    }
}

private val DialogPadding = PaddingValues(top = 24.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
