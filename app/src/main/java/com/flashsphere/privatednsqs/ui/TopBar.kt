package com.flashsphere.privatednsqs.ui

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.flashsphere.privatednsqs.R
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    showAppInfo: () -> Unit,
    openHelpDialog: (Boolean) -> Unit,
    requestAddTile: () -> Unit,
    backupConfig: (uri: Uri) -> Unit,
    restoreConfig: (uri: Uri) -> Unit,
    showSnackbarMessage: (message: SnackbarMessage) -> Unit,
) {
    val backupDestPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { backupConfig(it) }
    }
    val restoreFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { restoreConfig(it) }
    }
    TopAppBar(
        title = {
            Text(text = stringResource(R.string.app_name))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        actions = {
            val openDropdownMenu = rememberSaveable { mutableStateOf(false) }
            IconButton(onClick = { openDropdownMenu.value = true }) {
                Icon(Icons.Filled.MoreVert, null)
            }
            DropdownMenu(
                expanded = openDropdownMenu.value,
                onDismissRequest = { openDropdownMenu.value = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Info, stringResource(id = R.string.app_info)) },
                    text = { Text(stringResource(id = R.string.app_info))},
                    onClick = {
                        showAppInfo()
                        openDropdownMenu.value = false
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Filled.Add, stringResource(id = R.string.add_tile)) },
                        text = { Text(stringResource(id = R.string.add_tile))},
                        onClick = {
                            requestAddTile()
                            openDropdownMenu.value = false
                        }
                    )
                }
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Backup, stringResource(id = R.string.backup)) },
                    text = { Text(stringResource(id = R.string.backup))},
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        val timestamp = LocalDateTime.now().format(formatter)
                        runCatching {
                            backupDestPicker.launch("private-dns-qs-${timestamp}.txt")
                        }.onFailure {
                            Timber.e(it)
                            showSnackbarMessage(NoFilePicker(it.message))
                        }
                        openDropdownMenu.value = false
                    },
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Restore, stringResource(id = R.string.restore)) },
                    text = { Text(stringResource(id = R.string.restore))},
                    onClick = {
                        runCatching {
                            restoreFilePicker.launch(arrayOf("text/plain"))
                        }.onFailure {
                            Timber.e(it)
                            showSnackbarMessage(NoFilePicker(it.message))
                        }
                        openDropdownMenu.value = false
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(id = R.string.help)) },
                    text = { Text(stringResource(id = R.string.help))},
                    onClick = {
                        openHelpDialog(true)
                        openDropdownMenu.value = false
                    }
                )
            }
        }
    )
}