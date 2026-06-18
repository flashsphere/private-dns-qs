package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.util.suspendRunCatching
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDnsDialog(
    openDialog: MutableState<Boolean>,
    getSuggestions: (text: String) -> Set<String>,
    validate: (hostname: String) -> Boolean,
    addDns: (hostname: String) -> Unit,
) {
    if (openDialog.value) {
        DnsProviderDialog(
            initialHostname = "",
            getSuggestions = getSuggestions,
            validate = validate,
            onDismiss = { openDialog.value = false },
            onConfirm = { newHostname ->
                addDns(newHostname)
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
    updateDns: (index: Int, hostname: String) -> Unit,
) {
    openDialog.value?.let {
        val (index, dnsProvider) = it
        DnsProviderDialog(
            initialHostname = dnsProvider.hostname,
            getSuggestions = getSuggestions,
            validate = { hostname ->
                val trimmedHostname = hostname.trim()
                dnsProvider.hostname.equals(trimmedHostname, true) ||
                        validate(trimmedHostname)
            },
            onDismiss = { openDialog.value = null },
            onConfirm = { newHostname ->
                updateDns(index, newHostname)
                openDialog.value = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsProviderDialog(
    initialHostname: String = "",
    getSuggestions: (text: String) -> Set<String>,
    validate: (hostname: String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hostname: String) -> Unit,
) {
    val resources = LocalResources.current

    val textFieldState = rememberTextFieldState(initialText = initialHostname)

    val errorMessage = remember { mutableStateOf<String?>(null) }

    var expandSuggestions by remember { mutableStateOf(false) }
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

    CustomAlertDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onDismiss,
        content = {
            val focusRequester = remember { FocusRequester() }

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
                    onKeyboardAction = {
                        val hostname = textFieldState.text.toString()
                        if (hostname.isNotBlank() && validate(hostname)) {
                            onConfirm(textFieldState.text.toString())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(focusRequester)
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable),
                )
                ExposedDropdownMenu(
                    expanded = expandSuggestions,
                    onDismissRequest = { expandSuggestions = false }
                ) {
                    Column(
                        modifier = Modifier.exposedDropdownSize()
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
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            TextButton(
                enabled = textFieldState.text.isNotBlank() && errorMessage.value == null,
                onClick = {
                    val hostname = textFieldState.text.toString()
                    if (validate(hostname)) {
                        onConfirm(hostname)
                    }
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

private val spaceRegex = "\\s".toRegex()

@Preview
@Composable
private fun DnsProviderDialogPreview() {
    Surface(Modifier.fillMaxSize()) {
        DnsProviderDialog(
            initialHostname = "",
            getSuggestions = {
                setOf(
                    "one.one.one.one",
                    "two two two two two two two two two two two two two two two two two two " +
                            "two two two two two two two two two",
                    "three three three three three three three three three three three three"
                )
            },
            validate = { it.isBlank() || it == "test" },
            onDismiss = {},
            onConfirm = {},
        )
    }
}
