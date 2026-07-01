package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.ui.theme.AppTheme
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import com.flashsphere.privatednsqs.util.DnsConfiguration
import com.flashsphere.privatednsqs.util.absolutePathIfExists
import com.flashsphere.privatednsqs.util.iconsDir
import java.io.File


@Composable
fun SelectDnsDialog(
    configs: SnapshotStateList<DnsConfiguration>,
    currentConfig: DnsConfiguration,
    onSelect: (config: DnsConfiguration) -> Unit,
    openApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppTheme {
        Dialog(
            onDismissRequest = onDismiss,
            content = {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                ) {
                    SelectDnsDialogContent(
                        configs = configs,
                        currentConfig = currentConfig,
                        onSelect = onSelect,
                        openApp = openApp,
                        onDismiss = onDismiss,
                    )
                }
            },
        )
    }
}
@Composable
private fun SelectDnsDialogContent(
    configs: SnapshotStateList<DnsConfiguration>,
    currentConfig: DnsConfiguration,
    onSelect: (config: DnsConfiguration) -> Unit,
    openApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val selectedIndex = configs.indexOf(currentConfig)
    val backgroundColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceBright
    } else {
        MaterialTheme.colorScheme.surfaceDim
    }
    Column(modifier = Modifier
        .heightIn(max = 380.dp)
        .widthIn(max = 380.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(horizontal = 20.dp)
    ) {
        Box(Modifier.weight(weight = 1F, fill = false)) {
            if (configs.isEmpty()) {
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = configs,
                    key = { index, _ -> index },
                ) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (selectedIndex == index) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                backgroundColor
                            })
                            .clickable(
                                onClick = { onSelect(item) }
                            )
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item is DnsConfiguration.On) {
                            val iconPath = remember(item.icon) {
                                item.icon?.let { File(context.iconsDir, it).absolutePathIfExists }
                            }

                            if (iconPath != null) {
                                AsyncImage(
                                    modifier = Modifier.size(24.dp),
                                    model = iconPath,
                                    contentDescription = stringResource(R.string.icon),
                                    colorFilter = if (selectedIndex == index) {
                                        ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                                    } else {
                                        ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                        Text(
                            modifier = Modifier.weight(1F),
                            color = if (selectedIndex == index) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            text = if (item is DnsConfiguration.On) {
                                item.hostname
                            } else {
                                stringResource(item.mode.labelResId)
                            },
                            style = AppTypography.bodyMedium
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = openApp) { Text(stringResource(R.string.settings)) }
            Spacer(Modifier.weight(1F))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    }
}

@Preview
@Composable
private fun SelectDnsDialogPreview() {
    val configs = remember {
        mutableStateListOf(
            DnsConfiguration.Off,
            DnsConfiguration.Auto,
            DnsConfiguration.On("one", null),
            DnsConfiguration.On("two two two two two two two two two two two two " +
                    "two two two two two two two two two two two two two two two", null),
            DnsConfiguration.On("three", null),
            DnsConfiguration.On("four", null),
        )
    }
    Surface(Modifier.fillMaxSize()) {
        SelectDnsDialog(
            configs = configs,
            currentConfig = DnsConfiguration.On("one", null),
            onSelect = {},
            openApp = {},
            onDismiss = {},
        )
    }
}
