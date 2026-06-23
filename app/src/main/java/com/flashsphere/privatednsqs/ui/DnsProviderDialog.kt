package com.flashsphere.privatednsqs.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.util.FileUtils.toIconFile
import com.flashsphere.privatednsqs.util.absolutePathIfExists
import com.flashsphere.privatednsqs.util.suspendRunCatching
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDnsDialog(
    openDialog: MutableState<Boolean>,
    getSuggestions: (text: String) -> Set<String>,
    validate: (hostname: String) -> Boolean,
    processIcon: suspend (uri: Uri) -> File?,
    showToast: (message: String) -> Unit,
    addDns: (hostname: String, iconFile: File?) -> Unit,
) {
    if (openDialog.value) {
        DnsProviderDialog(
            initialHostname = "",
            initialIcon = null,
            getSuggestions = getSuggestions,
            validate = validate,
            processIcon = processIcon,
            showToast = showToast,
            onDismiss = { openDialog.value = false },
            onConfirm = { newHostname, newIcon ->
                addDns(newHostname, newIcon)
                openDialog.value = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDnsDialog(
    openDialog: MutableState<IndexedValue<DnsProvider>?>,
    getSuggestions: (text: String) -> Set<String>,
    validate: (hostname: String) -> Boolean,
    processIcon: suspend (uri: Uri) -> File?,
    showToast: (message: String) -> Unit,
    updateDns: (index: Int, hostname: String, iconFile: File?) -> Unit,
) {
    openDialog.value?.let {
        val (index, dnsProvider) = it
        DnsProviderDialog(
            initialHostname = dnsProvider.hostname,
            initialIcon = dnsProvider.icon,
            getSuggestions = getSuggestions,
            validate = { hostname ->
                val trimmedHostname = hostname.trim()
                dnsProvider.hostname.equals(trimmedHostname, true) ||
                        validate(trimmedHostname)
            },
            processIcon = processIcon,
            showToast = showToast,
            onDismiss = { openDialog.value = null },
            onConfirm = { newHostname, newIcon, ->
                updateDns(index, newHostname, newIcon)
                openDialog.value = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsProviderDialog(
    initialHostname: String = "",
    initialIcon: String?,
    getSuggestions: (text: String) -> Set<String>,
    validate: (hostname: String) -> Boolean,
    processIcon: suspend (uri: Uri) -> File?,
    showToast: (message: String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (hostname: String, iconFile: File?) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    val textFieldState = rememberTextFieldState(initialText = initialHostname)

    val errorMessage = remember { mutableStateOf<String?>(null) }

    val initialIconPath = remember(initialIcon) {
        initialIcon?.let { toIconFile(context, it).absolutePathIfExists }
    }
    var selectedIcon by rememberSaveable { mutableStateOf(initialIconPath) }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                selectedIcon = processIcon(uri)?.absolutePathIfExists
            }
        }
    }
    val onSubmit = {
        val hostname = textFieldState.text.toString()
        val iconFile = selectedIcon?.let { File(it) }
        if (hostname.isNotBlank() && validate(hostname)) {
            onConfirm(textFieldState.text.toString(), iconFile)
        }
    }

    CustomAlertDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onDismiss,
        content = {
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedIcon != null) {
                        AsyncImage(
                            modifier = Modifier.size(24.dp),
                            model = selectedIcon,
                            contentDescription = stringResource(R.string.icon),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_dns_on),
                            contentDescription = stringResource(R.string.icon),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Row {
                        TextButton(
                            onClick = {
                                runCatching { iconPicker.launch(arrayOf("image/png", "image/svg+xml")) }
                                    .onFailure {
                                        Timber.e(it)
                                        showToast(NoFilePicker(it.message).getMessage(resources))
                                    }
                            }
                        ) {
                            Text(stringResource(R.string.select_icon))
                        }
                        TextButton(
                            onClick = { selectedIcon = null }
                        ) {
                            Text(stringResource(R.string.default_icon))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                DnsHostnameTextField(
                    textFieldState = textFieldState,
                    errorMessage = errorMessage,
                    getSuggestions = getSuggestions,
                    validate = validate,
                    onConfirm = onSubmit,
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            TextButton(
                enabled = textFieldState.text.isNotBlank() && errorMessage.value == null,
                onClick = onSubmit,
            ) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsHostnameTextField(
    textFieldState: TextFieldState,
    errorMessage: MutableState<String?>,
    getSuggestions: (text: String) -> Set<String>,
    validate: (hostname: String) -> Boolean,
    onConfirm: () -> Unit,
) {
    val resources = LocalResources.current
    var expandSuggestions by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val suggestions = remember { mutableStateListOf<String>() }
    val suggestionsScrollState = rememberScrollState()

    @OptIn(FlowPreview::class)
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text }
            .debounce(300.milliseconds)
            .collect {
                val hostname = it.toString()
                if (!validate(hostname)) {
                    errorMessage.value = resources.getString(R.string.dns_provider_already_exists)
                } else {
                    errorMessage.value = null
                }

                suggestions.clear()
                suggestions.addAll(getSuggestions(hostname))

                launch { suspendRunCatching { suggestionsScrollState.scrollTo(0) } }
                expandSuggestions = suggestions.isNotEmpty()
            }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ExposedDropdownMenuBox(
        expanded = expandSuggestions,
        onExpandedChange = { expandSuggestions = it && suggestions.isNotEmpty() }
    ) {
        OutlinedTextField(
            state = textFieldState,
            placeholder = { Text(stringResource(R.string.dns_hostname_hint)) },
            isError = errorMessage.value != null,
            supportingText = errorMessage.value?.let {
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            inputTransformation = {
                val newText = asCharSequence()
                if (!originalText.contentEquals(newText) && newText.contains(spaceRegex)) {
                    val sanitized = newText.replace(spaceRegex, "")
                    var cursorIndex = originalSelection.start + sanitized.length - originalText.length
                    if (cursorIndex < 0 || cursorIndex > sanitized.length) cursorIndex = sanitized.length
                    replace(0, length, sanitized)
                    placeCursorBeforeCharAt(cursorIndex)
                }
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            onKeyboardAction = { onConfirm() },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expandSuggestions,
            onDismissRequest = { expandSuggestions = false }
        ) {
            Column(
                modifier = Modifier
                    .exposedDropdownSize()
                    .heightIn(max = 150.dp)
                    .verticalScroll(suggestionsScrollState)
            ) {
                suggestions.forEach { host ->
                    key(host) {
                        DropdownMenuItem(
                            text = {
                                Text(modifier = Modifier.padding(vertical = 4.dp), text = host)
                            },
                            onClick = {
                                textFieldState.setTextAndPlaceCursorAtEnd(host)
                                expandSuggestions = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private val spaceRegex = "\\s".toRegex()

@Preview
@Composable
private fun DnsProviderDialogPreview() {
    Surface(Modifier.fillMaxSize()) {
        DnsProviderDialog(
            initialHostname = "",
            initialIcon = null,
            getSuggestions = {
                setOf(
                    "one.one.one.one",
                    "two two two two two two two two two two two two two two two two two two " +
                            "two two two two two two two two two",
                    "three three three three three three three three three three three three"
                )
            },
            validate = { it.isBlank() || it == "test" },
            processIcon = { null },
            onDismiss = {},
            onConfirm = { _, _ -> },
            showToast = {},
        )
    }
}
