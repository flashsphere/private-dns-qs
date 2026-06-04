package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsProviderItem(
    dnsProvider: DnsProvider,
    index: Int,
    scope: ReorderableCollectionItemScope,
    onToggle: (checked: Boolean) -> Unit,
    canReorder: Boolean,
    onEdit: () -> Unit,
    onReorder: () -> Unit,
    onDelete: (index: Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val dismissState = rememberNoFlingSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeToDismissBackground(dismissState) },
        onDismiss = { onDelete(index) },
    ) {
        Row(modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onEdit)
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = dnsProvider.enabled, onCheckedChange = onToggle)
            Text(
                modifier = Modifier.weight(1F).padding(vertical = 4.dp),
                text = dnsProvider.hostname,
                style = AppTypography.bodyMedium
            )
            if (canReorder) {
                val tooltipState = rememberTooltipState()
                Tooltip(
                    state = tooltipState,
                    text = stringResource(R.string.drag_to_reorder),
                ) {
                    IconButton(
                        modifier = with(scope) {
                            Modifier
                                .draggableHandle(onDragStarted = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.GestureThresholdActivate)
                                }, onDragStopped = {
                                    onReorder()
                                })
                        },
                        onClick = {
                            if (!tooltipState.isVisible) {
                                coroutineScope.launch { tooltipState.show() }
                            }
                        },
                    ) {
                        Icon(imageVector = Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.drag_to_reorder))
                    }
                }
            }
        }
    }
}
