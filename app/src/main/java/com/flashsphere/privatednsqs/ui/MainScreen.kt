package com.flashsphere.privatednsqs.ui

import android.net.Uri
import androidx.activity.compose.ReportDrawn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.ui.theme.AppTheme
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.viewmodel.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    showAppInfo: () -> Unit,
    showMoreInfo: () -> Unit,
    requestAddTile: () -> Unit,
    showToast: (message: String) -> Unit,
) {
    MainScreen(
        openHelpDialogFlow = viewModel.openHelpDialogFlow,
        openHelpDialog = viewModel::openHelpDialog,
        snackbarMessageFlow = viewModel.snackbarMessages,
        dnsOffStateFlow = viewModel.dnsOffChecked,
        onDnsOffClick = viewModel::dnsOffChecked,
        dnsAutoStateFlow = viewModel.dnsAutoChecked,
        onDnsAutoClick = viewModel::dnsAutoChecked,
        dnsProviders = viewModel.dnsProviders,
        getDnsSuggestions = viewModel::getSuggestions,
        validateDnsProvider = viewModel::validateDnsProvider,
        addDnsProvider = viewModel::addDnsProvider,
        updateDnsProvider = viewModel::updateDnsProvider,
        toggleDnsProvider = viewModel::toggleDnsProvider,
        deleteDnsProvider = viewModel::deleteDnsProvider,
        restoreDnsProvider = viewModel::restoreDnsProvider,
        reorderDnsProvider = viewModel::reorderDnsProvider,
        reorderDnsProviders = viewModel::reorderDnsProviders,
        requireUnlockStateFlow = viewModel.requireUnlockChecked,
        onRequireUnlockClick = viewModel::requireUnlockChecked,
        showAppInfo = showAppInfo,
        showMoreInfo = showMoreInfo,
        requestAddTile = requestAddTile,
        backupConfig = viewModel::backup,
        restoreConfig = viewModel::restore,
        processIcon = viewModel::processSelectedIcon,
        showToast = showToast,
        showSnackbarMessage = viewModel::showSnackbarMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    openHelpDialogFlow: StateFlow<Boolean>,
    openHelpDialog: (Boolean) -> Unit,
    snackbarMessageFlow: Flow<SnackbarMessage>,
    dnsOffStateFlow: StateFlow<Boolean>,
    onDnsOffClick: (checked: Boolean) -> Unit,
    dnsAutoStateFlow: StateFlow<Boolean>,
    onDnsAutoClick: (checked: Boolean) -> Unit,
    dnsProviders: SnapshotStateList<DnsProvider>,
    getDnsSuggestions: (text: String) -> Set<String>,
    validateDnsProvider: (hostname: String) -> Boolean,
    addDnsProvider: (hostname: String, icon: File?) -> Unit,
    updateDnsProvider: (index: Int, hostname: String, icon: File?) -> Unit,
    toggleDnsProvider: (index: Int, enabled: Boolean) -> Unit,
    deleteDnsProvider: (index: Int) -> Unit,
    restoreDnsProvider: (index: Int, deleted: DnsProvider) -> Unit,
    reorderDnsProvider: (fromIndex: Int, toIndex: Int) -> Unit,
    reorderDnsProviders: () -> Unit,
    requireUnlockStateFlow: StateFlow<Boolean>,
    onRequireUnlockClick: (checked: Boolean) -> Unit,
    showAppInfo: () -> Unit,
    showMoreInfo: () -> Unit,
    requestAddTile: () -> Unit,
    backupConfig: (uri: Uri) -> Unit,
    restoreConfig: (uri: Uri) -> Unit,
    processIcon: suspend (uri: Uri) -> File?,
    showToast: (message: String) -> Unit,
    showSnackbarMessage: (message: SnackbarMessage) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val showAddDnsDialog = rememberSaveable { mutableStateOf(false) }
    val showEditDnsDialog = rememberSaveable(stateSaver = indexedValueSaver()) {
        mutableStateOf<IndexedValue<DnsProvider>?>(null)
    }

    LaunchedEffect(Unit) {
        val fileOperations = FileOperations()

        snackbarMessageFlow.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            // launch is needed so that the current snackbar can be dismissed while it is being shown,
            // since showSnackbar() suspends the coroutine
            launch {
                when (message) {
                    is NoPermissionMessage -> {
                        val result = snackbarHostState.showSnackbar(
                            message = message.getMessage(resources),
                            actionLabel = resources.getString(R.string.help),
                            duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) {
                            openHelpDialog(true)
                        }
                    }
                    is DnsProviderDeleted -> {
                        val result = snackbarHostState.showSnackbar(
                            message = message.getMessage(resources),
                            actionLabel = resources.getString(R.string.undo),
                            duration = SnackbarDuration.Long)
                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                restoreDnsProvider(message.index, message.dnsProvider)
                            }
                            SnackbarResult.Dismissed -> {
                                launch { fileOperations.delete(message.dnsProvider.icon) }
                            }
                        }
                    }
                    else -> {
                        snackbarHostState.showSnackbar(message.getMessage(resources))
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
                TopBar(
                    showAppInfo = showAppInfo,
                    openHelpDialog = openHelpDialog,
                    requestAddTile = requestAddTile,
                    backupConfig = backupConfig,
                    restoreConfig = restoreConfig,
                    showSnackbarMessage = showSnackbarMessage,
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) {
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(),
                        backgroundContent = {},
                        onDismiss = { snackbarHostState.currentSnackbarData?.dismiss() }
                    ) {
                        Snackbar(it)
                    }
                }
            }
        ) { padding ->
            val hapticFeedback = LocalHapticFeedback.current
            val lazyListState = rememberLazyListState()
            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                // minus 1 because we have one "fixed" item before the actual list
                reorderDnsProvider(from.index - 1, to.index - 1)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(bottom = 52.dp)
            ) {
                item(key = "header_modes", contentType = "header_modes") {
                    Column(Modifier.animateItem()) {
                        Header(stringResource(R.string.dns_modes_to_toggle))
                        DnsModeItem(dnsOffStateFlow, onDnsOffClick, stringResource(R.string.dns_off))
                        DnsModeItem(dnsAutoStateFlow, onDnsAutoClick, stringResource(R.string.dns_auto))
                    }
                }
                itemsIndexed(
                    items = dnsProviders,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "dns_provider" }
                ) { index, item ->
                    ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                        Surface(shadowElevation = elevation) {
                            DnsProviderItem(
                                dnsProvider = item,
                                index = index,
                                scope = this,
                                onToggle = { toggleDnsProvider(index, !item.enabled) },
                                canReorder = dnsProviders.size > 1,
                                onReorder = reorderDnsProviders,
                                onEdit = { showEditDnsDialog.value = IndexedValue(index, item) },
                                onDelete = deleteDnsProvider,
                            )
                        }
                    }
                }
                item(key = "dns_mode_add", contentType = "dns_mode_add") {
                    FilledTonalButton(
                        modifier = Modifier.animateItem().padding(horizontal = 4.dp),
                        onClick = { showAddDnsDialog.value = true },
                    ) {
                        Text(stringResource(R.string.add_dns_provider))
                    }
                }
                item(key = "other_settings", contentType = "other_settings") {
                    Column(Modifier.animateItem()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Header(stringResource(R.string.other_settings))
                        DnsModeItem(requireUnlockStateFlow, onRequireUnlockClick,
                            stringResource(R.string.require_unlock))
                    }
                }
            }
            AddDnsDialog(
                openDialog = showAddDnsDialog,
                getSuggestions = getDnsSuggestions,
                validate = validateDnsProvider,
                processIcon = processIcon,
                showToast = showToast,
                addDns = addDnsProvider,
            )
            EditDnsDialog(
                openDialog = showEditDnsDialog,
                getSuggestions = getDnsSuggestions,
                validate = validateDnsProvider,
                processIcon = processIcon,
                showToast = showToast,
                updateDns = updateDnsProvider,
            )
            HelpDialog(
                openHelpDialogFlow = openHelpDialogFlow,
                openHelpDialog = openHelpDialog,
                showMoreInfo = showMoreInfo,
            )
        }
    }
    ReportDrawn()
}

@Composable
private fun Header(text: String) {
    val locale = LocalLocale.current
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        text = text.uppercase(locale.platformLocale),
        style = AppTypography.bodyMedium,
        fontWeight = FontWeight.Bold,
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = .5F))
}

@Preview
@Composable
private fun MainScreenPreview() {
    val openHelpDialogFlow = remember { MutableStateFlow(false) }
    val snackbarMessageFlow = remember { MutableStateFlow(null).filterNotNull() }
    val dnsProviders = remember { mutableStateListOf(DnsProvider(id = 0, hostname = "one.one.one.one", icon = null)) }
    val dnsOff = remember { MutableStateFlow(true) }
    val dnsAuto = remember { MutableStateFlow(true) }
    val requireUnlock = remember { MutableStateFlow(false) }

    MainScreen(
        openHelpDialogFlow = openHelpDialogFlow,
        openHelpDialog = { openHelpDialogFlow.value = it },
        snackbarMessageFlow = snackbarMessageFlow,
        dnsOffStateFlow = dnsOff,
        onDnsOffClick = { dnsOff.value = it },
        dnsAutoStateFlow = dnsAuto,
        onDnsAutoClick = { dnsAuto.value = it },
        dnsProviders = dnsProviders,
        getDnsSuggestions = { emptySet() },
        validateDnsProvider = { true },
        addDnsProvider = { _, _ -> },
        updateDnsProvider = { _, _, _ -> },
        toggleDnsProvider = { _, _ -> },
        deleteDnsProvider = {},
        restoreDnsProvider = { _, _ -> },
        reorderDnsProvider = { _, _ -> },
        reorderDnsProviders = {},
        requireUnlockStateFlow = requireUnlock,
        onRequireUnlockClick = { requireUnlock.value = it },
        showAppInfo = {},
        showMoreInfo = {},
        requestAddTile = {},
        backupConfig = {},
        restoreConfig = {},
        processIcon = { null },
        showToast = {},
        showSnackbarMessage = {},
    )
}