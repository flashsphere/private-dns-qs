package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import com.flashsphere.privatednsqs.util.absolutePathIfExists
import com.flashsphere.privatednsqs.util.iconsDir
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import java.io.File

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dismissState = rememberNoFlingSwipeToDismissBoxState()

    val iconPath = remember(dnsProvider.icon) {
        dnsProvider.icon?.let { File(context.iconsDir, it).absolutePathIfExists }
    }

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

            if (iconPath != null) {
                AsyncImage(
                    modifier = Modifier.size(24.dp),
                    model = iconPath,
                    contentDescription = stringResource(R.string.icon),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                )
                Spacer(Modifier.width(8.dp))
            }

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
                        Icon(painter = painterResource(R.drawable.ic_drag_handle),
                            contentDescription = stringResource(R.string.drag_to_reorder))
                    }
                }
            }
        }
    }
}
