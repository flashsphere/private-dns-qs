package com.flashsphere.privatednsqs.ui

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
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
    toastActions: ToastActions,
    showSnackbarMessage: (message: SnackbarMessage) -> Unit,
) {
    val resources = LocalResources.current
    val backupDestPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        toastActions.cancelToast()
        uri?.let { backupConfig(it) }
    }
    val restoreFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        toastActions.cancelToast()
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
                Icon(painterResource(R.drawable.ic_more_vert), null)
            }
            DropdownMenu(
                expanded = openDropdownMenu.value,
                onDismissRequest = { openDropdownMenu.value = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(painterResource(R.drawable.ic_info),
                        stringResource(id = R.string.app_info)) },
                    text = { Text(stringResource(id = R.string.app_info))},
                    onClick = {
                        showAppInfo()
                        openDropdownMenu.value = false
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(painterResource(R.drawable.ic_add),
                            stringResource(id = R.string.add_tile)) },
                        text = { Text(stringResource(id = R.string.add_tile))},
                        onClick = {
                            requestAddTile()
                            openDropdownMenu.value = false
                        }
                    )
                }
                DropdownMenuItem(
                    leadingIcon = { Icon(painterResource(R.drawable.ic_backup),
                        stringResource(id = R.string.backup)) },
                    text = { Text(stringResource(id = R.string.backup))},
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        val timestamp = LocalDateTime.now().format(formatter)
                        runCatching {
                            toastActions.showToast(resources.getString(R.string.toast_backup_help))
                            backupDestPicker.launch("private-dns-qs-${timestamp}.txt")
                        }.onFailure {
                            Timber.e(it)
                            showSnackbarMessage(NoFilePicker(it.message))
                        }
                        openDropdownMenu.value = false
                    },
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(painterResource(R.drawable.ic_restore),
                        stringResource(id = R.string.restore)) },
                    text = { Text(stringResource(id = R.string.restore))},
                    onClick = {
                        runCatching {
                            toastActions.showToast(resources.getString(R.string.toast_restore_help))
                            restoreFilePicker.launch(arrayOf("text/plain"))
                        }.onFailure {
                            Timber.e(it)
                            showSnackbarMessage(NoFilePicker(it.message))
                        }
                        openDropdownMenu.value = false
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(painterResource(R.drawable.ic_help),
                        stringResource(id = R.string.help)) },
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