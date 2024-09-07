package com.flashsphere.privatednsqs.ui

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.MainViewModel
import com.flashsphere.privatednsqs.PrivateDnsConstants.HELP_URL
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.ui.theme.AppTheme
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import com.flashsphere.privatednsqs.ui.theme.Monospace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    showAppInfo: () -> Unit,
    requestAddTile: () -> Unit,
) {
    MainScreen(
        hasPermission = viewModel::hasPermission,
        snackbarMessageFlow = viewModel.snackbarMessages,
        dnsOffStateFlow = viewModel.dnsOffChecked,
        onDnsOffClick = viewModel::dnsOffChecked,
        dnsAutoStateFlow = viewModel.dnsAutoChecked,
        onDnsAutoClick = viewModel::dnsAutoChecked,
        dnsOnStateFlow = viewModel.dnsOnChecked,
        onDnsOnClick = viewModel::dnsOnChecked,
        dnsHostnameTextFieldState = viewModel.dnsHostnameTextFieldState,
        requireUnlockStateFlow = viewModel.requireUnlockChecked,
        onRequireUnlockClick = viewModel::requireUnlockChecked,
        onSaveClick = viewModel::save,
        showAppInfo = showAppInfo,
        requestAddTile = requestAddTile,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    hasPermission: () -> Boolean,
    snackbarMessageFlow: Flow<SnackbarMessage>,
    dnsOffStateFlow: StateFlow<Boolean>,
    onDnsOffClick: (checked: Boolean) -> Unit,
    dnsAutoStateFlow: StateFlow<Boolean>,
    onDnsAutoClick: (checked: Boolean) -> Unit,
    dnsOnStateFlow: StateFlow<Boolean>,
    onDnsOnClick: (checked: Boolean) -> Unit,
    dnsHostnameTextFieldState: TextFieldState,
    requireUnlockStateFlow: StateFlow<Boolean>,
    onRequireUnlockClick: (checked: Boolean) -> Unit,
    onSaveClick: () -> Unit,
    showAppInfo: () -> Unit,
    requestAddTile: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val openHelpMenu = rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!hasPermission()) {
            openHelpMenu.value = true
        }
    }
    LaunchedEffect(Unit) {
        snackbarMessageFlow.collect {
            if (it is NoDnsHostnameMessage) {
                focusRequester.requestFocus()
            }

            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                if (it is NoPermissionMessage) {
                    val result = snackbarHostState.showSnackbar(
                        message = it.getMessage(context),
                        actionLabel = context.getString(R.string.help))
                    if (result == SnackbarResult.ActionPerformed) {
                        openHelpMenu.value = true
                    }
                } else {
                    snackbarHostState.showSnackbar(it.getMessage(context))
                }
            }
        }
    }

    AppTheme {
        Scaffold (
            modifier = Modifier.imePadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(R.string.app_name))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                text = { Text(stringResource(id = R.string.app_info))},
                                onClick = {
                                    showAppInfo()
                                    openDropdownMenu.value = false
                                }
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.add_tile))},
                                    onClick = {
                                        requestAddTile()
                                        openDropdownMenu.value = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.help))},
                                onClick = {
                                    openHelpMenu.value = true
                                    openDropdownMenu.value = false
                                }
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) {
                val state = rememberSwipeToDismissBoxState(
                    confirmValueChange = { state ->
                        if (state != SwipeToDismissBoxValue.Settled) {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(state = state, backgroundContent = {}) {
                    Snackbar(it)
                }
            } }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Header(stringResource(R.string.dns_modes_to_toggle))

                CheckBoxWithLabel(dnsOffStateFlow, onDnsOffClick, stringResource(R.string.qt_off))
                CheckBoxWithLabel(dnsAutoStateFlow, onDnsAutoClick, stringResource(R.string.qt_auto))

                Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CheckBoxWithLabel(dnsOnStateFlow, onDnsOnClick, stringResource(R.string.dns_on))
                    TextField(modifier = Modifier.focusRequester(focusRequester),
                        textFieldState = dnsHostnameTextFieldState,
                        label = stringResource(R.string.dns_hostname_hint))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Header(stringResource(R.string.other_settings))

                CheckBoxWithLabel(requireUnlockStateFlow, onRequireUnlockClick, stringResource(R.string.require_unlock))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSaveClick()
                    },
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)) {
                    Text(text = stringResource(R.string.save))
                }

                Spacer(modifier = Modifier.height(52.dp))
            }

            HelpDialog(openHelpMenu)
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        text = text.uppercase(),
        style = AppTypography.bodyMedium,
        fontWeight = FontWeight.Bold,
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = .5F))
}

@Composable
private fun CheckBoxWithLabel(
    state: StateFlow<Boolean>,
    onClick: (checked: Boolean) -> Unit,
    label: String,
) {
    val checked = state.collectAsStateWithLifecycle().value
    Row(modifier = Modifier
        .padding(horizontal = 4.dp)
        .clickable { onClick(!checked) },
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onClick)
        Text(modifier = Modifier.padding(end = 8.dp), text = label, style = AppTypography.bodyMedium)
    }
}

val spaceRegex = "\\s".toRegex()

@Composable
private fun TextField(
    modifier: Modifier,
    textFieldState: TextFieldState,
    label: String,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        state = textFieldState,
        textStyle = AppTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
        inputTransformation = {
            val newText = asCharSequence()
            if (originalText != newText && newText.contains(spaceRegex)) {
                val sanitized = newText.replace(spaceRegex, "")
                var cursorIndex = originalSelection.start + sanitized.length - originalText.length
                if (cursorIndex < 0 || cursorIndex > sanitized.length) cursorIndex = sanitized.length
                replace(0, length, sanitized)
                placeCursorBeforeCharAt(cursorIndex)
            }
        },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done),
        onKeyboardAction = {
            focusManager.clearFocus()
            keyboardController?.hide()
        },
        decorator = { innerTextField ->
            Column(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()) {
                Box(contentAlignment = Alignment.CenterStart) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = label,
                            style = AppTypography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.5F)
                        )
                    }
                    innerTextField()

                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpDialog(openDialog: MutableState<Boolean>) {
    if (openDialog.value) {
        val context = LocalContext.current
        BasicAlertDialog(onDismissRequest = { openDialog.value = false }) {
            Surface(
                modifier = Modifier.wrapContentSize(),
                shape = MaterialTheme.shapes.extraLarge,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = stringResource(R.string.message_help),
                        style = AppTypography.bodyMedium)
                    SelectionContainer {
                        Text(text = stringResource(R.string.message_help_adb, context.packageName),
                            fontFamily = Monospace,
                            style = AppTypography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        TextButton(onClick = {
                            context.startActivity(Intent(ACTION_VIEW, HELP_URL))
                        }) { Text(stringResource(R.string.more_details)) }
                        Spacer(modifier = Modifier.weight(1F))
                        TextButton(onClick = { openDialog.value = false }) { Text(stringResource(R.string.ok)) }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    val snackbarMessageFlow = MutableStateFlow(null).filterNotNull()
    val dnsOff = MutableStateFlow(true)
    val dnsAuto = MutableStateFlow(true)
    val dnsOn = MutableStateFlow(true)
    val dnsHostName = TextFieldState()
    val requireUnlock = MutableStateFlow(false)

    MainScreen(
        hasPermission = { true },
        snackbarMessageFlow = snackbarMessageFlow,
        dnsOffStateFlow = dnsOff,
        onDnsOffClick = { dnsOff.value = it },
        dnsAutoStateFlow = dnsAuto,
        onDnsAutoClick = { dnsAuto.value = it },
        dnsOnStateFlow = dnsOn,
        onDnsOnClick = { dnsOn.value = it },
        dnsHostnameTextFieldState = dnsHostName,
        requireUnlockStateFlow = requireUnlock,
        onRequireUnlockClick = { requireUnlock.value = it },
        onSaveClick = { ChangesSavedMessage },
        showAppInfo = {},
        requestAddTile = {},
    )
}