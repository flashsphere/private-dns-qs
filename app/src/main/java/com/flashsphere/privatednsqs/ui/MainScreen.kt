package com.flashsphere.privatednsqs.ui

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Build
import androidx.activity.compose.ReportDrawn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.privatednsqs.PrivateDnsConstants.HELP_URL
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.ui.theme.AppTheme
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import com.flashsphere.privatednsqs.ui.theme.Monospace
import com.flashsphere.privatednsqs.viewmodel.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    showAppInfo: () -> Unit,
    requestAddTile: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        viewModel.reloadDnsHostname()
        onPauseOrDispose {}
    }
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
        dnsHostnameFlow = viewModel.dnsHostname,
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
    dnsHostnameFlow: StateFlow<String>,
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

    LifecycleResumeEffect(Unit) {
        openHelpMenu.value = !hasPermission()
        onPauseOrDispose {}
    }
    LaunchedEffect(Unit) {
        launch {
            snackbarMessageFlow.collect {
                if (it is NoDnsHostnameMessage) {
                    focusRequester.requestFocus()
                }

                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    if (it is NoPermissionMessage) {
                        val result = snackbarHostState.showSnackbar(
                            message = it.getMessage(context),
                            actionLabel = context.getString(R.string.help),
                            duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) {
                            openHelpMenu.value = true
                        }
                    } else {
                        snackbarHostState.showSnackbar(it.getMessage(context))
                    }
                }
            }
        }
    }

    val windowInsetsPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

    AppTheme {
        Scaffold (
            contentWindowInsets = WindowInsets.ime,
            modifier = Modifier.windowInsetsPadding(windowInsetsPadding),
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
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(id = R.string.help)) },
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
                    .consumeWindowInsets(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                Header(stringResource(R.string.dns_modes_to_toggle))

                CheckBoxWithLabel(dnsOffStateFlow, onDnsOffClick, stringResource(R.string.dns_off))
                CheckBoxWithLabel(dnsAutoStateFlow, onDnsAutoClick, stringResource(R.string.dns_auto))

                Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CheckBoxWithLabel(dnsOnStateFlow, onDnsOnClick, stringResource(R.string.dns_on))
                    TextField(
                        modifier = Modifier.focusRequester(focusRequester),
                        textFieldState = dnsHostnameTextFieldState,
                        label = stringResource(R.string.dns_hostname_hint),
                        trailingIcon = { RevertIcon(dnsHostnameFlow, dnsHostnameTextFieldState) }
                    )
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
    ReportDrawn()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextField(
    modifier: Modifier,
    textFieldState: TextFieldState,
    label: String,
    trailingIcon: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val wasImeVisible = remember { mutableStateOf(imeVisible) }

    LaunchedEffect(imeVisible) {
        if (!imeVisible && wasImeVisible.value) {
            focusManager.clearFocus()
        }
        wasImeVisible.value = imeVisible
    }

    BasicTextField(
        modifier = modifier.fillMaxWidth(),
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
        decorator = { innerTextField ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1F)) {
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
                trailingIcon()
            }
        }
    )
}

@Composable
private fun RevertIcon(
    dnsHostnameFlow: StateFlow<String>,
    textFieldState: TextFieldState,
) {
    val revertState = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            snapshotFlow { textFieldState.text }
                .map { it.toString() != dnsHostnameFlow.value }
                .collect { revertState.value = it }
        }
        launch {
            dnsHostnameFlow
                .map { it != textFieldState.text.toString() }
                .collect { revertState.value = it }
        }
    }

    if (revertState.value) {
        Tooltip(stringResource(R.string.revert)) {
            Box(modifier = Modifier.size(24.dp).padding(start = 4.dp)
                .clickable(
                    interactionSource = null,
                    indication = ripple(bounded = false),
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd(dnsHostnameFlow.value)
                    }
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = stringResource(R.string.revert)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tooltip(
    text: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip { Text(text = text, style = AppTypography.bodyMedium) }
        },
        state = rememberTooltipState()
    ) {
        content()
    }
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
                            runCatching { context.startActivity(Intent(ACTION_VIEW, HELP_URL)) }
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
    val snackbarMessageFlow = remember { MutableStateFlow(null).filterNotNull() }
    val dnsOff = remember { MutableStateFlow(true) }
    val dnsAuto = remember { MutableStateFlow(true) }
    val dnsOn = remember { MutableStateFlow(true) }
    val dnsHostnameTextFieldState = TextFieldState()
    val requireUnlock = remember { MutableStateFlow(false) }
    val dnsHostnameFlow = remember { MutableStateFlow("") }

    MainScreen(
        hasPermission = { true },
        snackbarMessageFlow = snackbarMessageFlow,
        dnsOffStateFlow = dnsOff,
        onDnsOffClick = { dnsOff.value = it },
        dnsAutoStateFlow = dnsAuto,
        onDnsAutoClick = { dnsAuto.value = it },
        dnsOnStateFlow = dnsOn,
        onDnsOnClick = { dnsOn.value = it },
        dnsHostnameTextFieldState = dnsHostnameTextFieldState,
        dnsHostnameFlow = dnsHostnameFlow,
        requireUnlockStateFlow = requireUnlock,
        onRequireUnlockClick = { requireUnlock.value = it },
        onSaveClick = { dnsHostnameFlow.value = dnsHostnameTextFieldState.text.toString() },
        showAppInfo = {},
        requestAddTile = {},
    )
}