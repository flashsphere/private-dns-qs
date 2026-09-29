package com.flashsphere.privatednsqs.ui

import androidx.activity.compose.ReportDrawn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.ui.theme.AppTheme
import com.flashsphere.privatednsqs.ui.theme.AppTypography
import com.flashsphere.privatednsqs.util.absolutePathIfExists
import com.flashsphere.privatednsqs.util.iconsDir
import com.flashsphere.privatednsqs.viewmodel.AppShortcutsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppShortcutsScreen(
    viewModel: AppShortcutsViewModel,
    onBack: () -> Unit,
) {
    AppShortcutsScreen(
        dnsOffStateFlow = viewModel.dnsOffShortcutStateFlow,
        onDnsOffChecked = viewModel::updateDnsOffShortcut,
        onPinDnsOff = viewModel::pinDnsOffShortcut,
        dnsAutoStateFlow = viewModel.dnsAutoShortcutStateFlow,
        onDnsAutoChecked = viewModel::updateDnsAutoShortcut,
        onPinDnsAuto = viewModel::pinDnsAutoShortcut,
        dnsToggleStateFlow = viewModel.dnsToggleShortcutStateFlow,
        onDnsToggleChecked = viewModel::updateDnsToggleShortcut,
        onPinDnsToggle = viewModel::pinDnsToggleShortcut,
        dnsProvidersStateFlow = viewModel.dnsProviders,
        onDnsProviderChecked = viewModel::updateDnsProviderShortcut,
        onPinDnsProvider = viewModel::pinDnsProviderShortcut,
        launcherIconVisibleStateFlow = viewModel.launcherIconVisibleStateFlow,
        toggleLauncherIcon = viewModel::toggleLauncherIcon,
        pinShortcutSupported = viewModel.pinShortcutSupported,
        shortcutCountWarningFlow = viewModel.shortcutCountWarningFlow,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShortcutsScreen(
    dnsOffStateFlow: StateFlow<Boolean>,
    onDnsOffChecked: (checked: Boolean) -> Unit,
    onPinDnsOff: () -> Unit,
    dnsAutoStateFlow: StateFlow<Boolean>,
    onDnsAutoChecked: (checked: Boolean) -> Unit,
    onPinDnsAuto: () -> Unit,
    dnsToggleStateFlow: StateFlow<Boolean>,
    onDnsToggleChecked: (checked: Boolean) -> Unit,
    onPinDnsToggle: () -> Unit,
    dnsProvidersStateFlow: StateFlow<List<DnsProvider>>,
    onDnsProviderChecked: (index: Int, checked: Boolean) -> Unit,
    onPinDnsProvider: (index: Int) -> Unit,
    launcherIconVisibleStateFlow: StateFlow<Boolean>,
    toggleLauncherIcon: () -> Unit,
    pinShortcutSupported: Boolean,
    shortcutCountWarningFlow: StateFlow<String>,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val windowInsetsPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    var snackbarHeight by remember { mutableIntStateOf(0) }

    val shortcutsSupported = launcherIconVisibleStateFlow.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        shortcutCountWarningFlow.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            if (msg.isNotBlank()) {
                launch {
                    snackbarHostState.showSnackbar(
                        message = msg,
                        duration = SnackbarDuration.Indefinite,
                    )
                }
            }
        }
    }
    AppTheme {
        Scaffold (
            contentWindowInsets = WindowInsets.ime,
            modifier = Modifier.windowInsetsPadding(windowInsetsPadding),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), null)
                        }
                    },
                    title = {
                        Text(text = stringResource(R.string.app_shortcuts))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    actions = {
                        LauncherMenuItem(
                            stateFlow = launcherIconVisibleStateFlow,
                            onClick = toggleLauncherIcon,
                        )
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.onSizeChanged { size -> snackbarHeight = size.height }
                ) {
                    Snackbar(it)
                }
            }
        ) { padding ->
            val dnsProviders = dnsProvidersStateFlow.collectAsStateWithLifecycle().value

            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(bottom = with(LocalDensity.current) {
                    snackbarHeight.toDp()
                })
            ) {
                item(key = "header_modes", contentType = "header_modes") {
                    Column(Modifier.animateItem()) {
                        DnsModeShortcutItem(
                            state = dnsOffStateFlow,
                            label = stringResource(R.string.dns_off),
                            onChecked = onDnsOffChecked,
                            onPin = onPinDnsOff,
                            shortcutsSupported = shortcutsSupported,
                            pinSupported = pinShortcutSupported,
                        )
                        DnsModeShortcutItem(
                            state = dnsAutoStateFlow,
                            label = stringResource(R.string.dns_auto),
                            onChecked = onDnsAutoChecked,
                            onPin = onPinDnsAuto,
                            shortcutsSupported = shortcutsSupported,
                            pinSupported = pinShortcutSupported,
                        )
                        DnsModeShortcutItem(
                            state = dnsToggleStateFlow,
                            label = stringResource(R.string.dns_toggle),
                            onChecked = onDnsToggleChecked,
                            onPin = onPinDnsToggle,
                            shortcutsSupported = shortcutsSupported,
                            pinSupported = pinShortcutSupported,
                        )
                    }
                }
                itemsIndexed(
                    items = dnsProviders,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "dns_provider" }
                ) { index, item ->
                    DnsProviderShortcutItem(
                        dnsProvider = item,
                        onChecked = { onDnsProviderChecked(index, !item.shortcutEnabled) },
                        onPin = { onPinDnsProvider(index) },
                        shortcutsSupported = shortcutsSupported,
                        pinSupported = pinShortcutSupported,
                    )
                }
            }
        }
    }
    ReportDrawn()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsModeShortcutItem(
    state: StateFlow<Boolean>,
    label: String,
    onChecked: (checked: Boolean) -> Unit,
    onPin: () -> Unit,
    shortcutsSupported: Boolean,
    pinSupported: Boolean,
) {
    val checked = state.collectAsStateWithLifecycle().value
    val checkboxInteractionSource = remember { MutableInteractionSource() }
    Row(modifier = Modifier
        .focusProperties { canFocus = false }
        .clickable(
            interactionSource = checkboxInteractionSource,
            indication = null,
            enabled = shortcutsSupported,
            onClick = { onChecked(!checked) },
        )
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(enabled = shortcutsSupported, checked = checked, onCheckedChange = onChecked,
            interactionSource = checkboxInteractionSource)
        Text(modifier = Modifier.weight(1F), text = label, style = AppTypography.bodyMedium)
        if (pinSupported) {
            Tooltip(
                state = rememberTooltipState(),
                text = stringResource(R.string.pin_to_home),
            ) {
                IconButton(
                    onClick = onPin,
                ) {
                    Icon(painter = painterResource(R.drawable.ic_pin_to_home),
                        contentDescription = stringResource(R.string.pin_to_home))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsProviderShortcutItem(
    dnsProvider: DnsProvider,
    onChecked: (checked: Boolean) -> Unit,
    onPin: (dnsProvider: DnsProvider) -> Unit,
    shortcutsSupported: Boolean,
    pinSupported: Boolean,
) {
    val context = LocalContext.current
    val checkboxInteractionSource = remember { MutableInteractionSource() }

    Row(modifier = Modifier
        .clickable(
            interactionSource = checkboxInteractionSource,
            indication = null,
            enabled = shortcutsSupported,
            onClick = { onChecked(!dnsProvider.shortcutEnabled) },
        )
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(enabled = shortcutsSupported, checked = dnsProvider.shortcutEnabled,
            onCheckedChange = onChecked, interactionSource = checkboxInteractionSource)

        val iconPath = remember(dnsProvider.icon) {
            dnsProvider.icon?.let { File(context.iconsDir, it).absolutePathIfExists }
        }
        if (iconPath != null) {
            AsyncImage(
                modifier = Modifier.size(24.dp),
                model = iconPath,
                contentDescription = stringResource(R.string.icon),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(Modifier.weight(1F).padding(vertical = 4.dp)) {
            var hostnameStyle = AppTypography.bodyMedium
            if (!dnsProvider.label.isNullOrBlank()) {
                hostnameStyle = AppTypography.bodySmall

                Text(
                    text = dnsProvider.label,
                    style = AppTypography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = dnsProvider.hostname,
                style = hostnameStyle
            )
        }

        if (pinSupported) {
            Tooltip(
                state = rememberTooltipState(),
                text = stringResource(R.string.pin_to_home),
            ) {
                IconButton(
                    onClick = { onPin(dnsProvider) },
                ) {
                    Icon(painter = painterResource(R.drawable.ic_pin_to_home),
                        contentDescription = stringResource(R.string.pin_to_home))
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppShortcutsScreenPreview() {
    val dnsOffStateFlow = remember { MutableStateFlow(false) }
    val dnsAutoStateFlow = remember { MutableStateFlow(false) }
    val dnsToggleStateFlow = remember { MutableStateFlow(false) }
    val launcherIconVisibleStateFlow = remember { MutableStateFlow(false) }
    val dnsProviders = remember { MutableStateFlow(
        mutableStateListOf(
            DnsProvider(id = 1, hostname = "one.one.one.one", label = null, icon = null),
            DnsProvider(id = 2, hostname = "dns.google", label = "Google", icon = null),
        )
    ) }
    val shortcutCountWarningFlow = remember { MutableStateFlow("some message") }
    AppShortcutsScreen(
        dnsOffStateFlow = dnsOffStateFlow,
        onDnsOffChecked = { dnsOffStateFlow.value = it },
        onPinDnsOff = {},
        dnsAutoStateFlow = dnsAutoStateFlow,
        onDnsAutoChecked = { dnsAutoStateFlow.value = it },
        onPinDnsAuto = {},
        dnsToggleStateFlow = dnsToggleStateFlow,
        onDnsToggleChecked = { dnsToggleStateFlow.value = it },
        onPinDnsToggle = {},
        dnsProvidersStateFlow = dnsProviders,
        onDnsProviderChecked = { _, _ -> },
        onPinDnsProvider = {},
        launcherIconVisibleStateFlow = launcherIconVisibleStateFlow,
        toggleLauncherIcon = {},
        pinShortcutSupported = true,
        shortcutCountWarningFlow = shortcutCountWarningFlow,
        onBack = {},
    )
}
