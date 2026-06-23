package com.flashsphere.privatednsqs.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.flashsphere.privatednsqs.PrivateDnsApplication
import com.flashsphere.privatednsqs.backup.DnsProviderSnapshot
import com.flashsphere.privatednsqs.backup.SettingsSnapshot
import com.flashsphere.privatednsqs.backup.SettingsSnapshotV1
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.datastore.IdGenerator
import com.flashsphere.privatednsqs.datastore.ImageIdGenerator
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.dnsAutoToggle
import com.flashsphere.privatednsqs.datastore.dnsOffToggle
import com.flashsphere.privatednsqs.datastore.dnsProviders
import com.flashsphere.privatednsqs.datastore.dnsProvidersFlow
import com.flashsphere.privatednsqs.datastore.getStateFlow
import com.flashsphere.privatednsqs.datastore.json
import com.flashsphere.privatednsqs.datastore.requireUnlock
import com.flashsphere.privatednsqs.ui.BackupCompleted
import com.flashsphere.privatednsqs.ui.BackupFailed
import com.flashsphere.privatednsqs.ui.DnsProviderDeleted
import com.flashsphere.privatednsqs.ui.RestoreCompleted
import com.flashsphere.privatednsqs.ui.RestoreFailed
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.util.FileUtils.delete
import com.flashsphere.privatednsqs.util.FileUtils.move
import com.flashsphere.privatednsqs.util.FileUtils.toIconFile
import com.flashsphere.privatednsqs.util.FileUtils.write
import com.flashsphere.privatednsqs.util.absolutePathIfExists
import com.flashsphere.privatednsqs.util.base64DecodeToFile
import com.flashsphere.privatednsqs.util.suspendRunCatching
import com.flashsphere.privatednsqs.util.toBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.days

class MainViewModel(
    private val application: PrivateDnsApplication,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val dataStore = application.dataStore
    private val json = application.json
    private val contentResolver = application.contentResolver
    private val privateDns = PrivateDns(application)

    val snackbarMessages: SharedFlow<SnackbarMessage>
        field = MutableSharedFlow<SnackbarMessage>(
            replay = 0,
            extraBufferCapacity = 1,
            BufferOverflow.SUSPEND
        )

    private val _openHelpDialogFlow = savedStateHandle.getMutableStateFlow("open_help_menu", false)
    val openHelpDialogFlow = _openHelpDialogFlow.asStateFlow()

    val dnsOffChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_OFF_TOGGLE)
    val dnsAutoChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.DNS_AUTO_TOGGLE)
    val dnsProviders = mutableStateListOf<DnsProvider>()
    val requireUnlockChecked = dataStore.getStateFlow(viewModelScope, PreferenceKeys.REQUIRE_UNLOCK)

    init {
        dataStore.dnsProvidersFlow()
            .onEach { list ->
                dnsProviders.clear()
                dnsProviders.addAll(list)
            }
            .launchIn(viewModelScope)

        _openHelpDialogFlow.value = !hasPermission()
        cleanupOrphanImages()
    }

    fun openHelpDialog(open: Boolean) {
        _openHelpDialogFlow.value = open
    }

    fun dnsOffChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsOffToggle(checked) }
    }

    fun dnsAutoChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.dnsAutoToggle(checked) }
    }

    fun requireUnlockChecked(checked: Boolean) {
        viewModelScope.launch { dataStore.requireUnlock(checked) }
    }

    fun showSnackbarMessage(message: SnackbarMessage) {
        viewModelScope.launch {
            // wait until there's at least 1 subscriber before emitting
            snackbarMessages.subscriptionCount.first { it > 0 }
            snackbarMessages.emit(message)
        }
    }

    fun hasPermission(): Boolean {
        return privateDns.hasPermission()
    }

    fun validateDnsProvider(hostname: String): Boolean {
        val trimmedHost = hostname.trim()
        if (trimmedHost.isEmpty()) return true

        return !dnsProviders.any { it.hostname.equals(trimmedHost, true) }
    }

    fun addDnsProvider(hostname: String, iconFile: File? = null) {
        val trimmedHost = hostname.trim()
        if (trimmedHost.isEmpty()) return

        val providers = dnsProviders.toMutableList()
        viewModelScope.launch {
            Timber.d("Adding '%s'", hostname)
            val dnsProvider = createDnsProvider(trimmedHost, true, iconFile)
            providers.add(dnsProvider)
            dataStore.dnsProviders(providers)
        }
    }

    suspend fun createDnsProvider(hostname: String, enabled: Boolean = true, iconFile: File? = null): DnsProvider {
        val id = IdGenerator.getNextId(dataStore)

        val iconFilename = if (iconFile != null && iconFile.exists()) {
            val filename = iconFile.name.ifBlank { "${ImageIdGenerator.getNextId(dataStore)}.png" }
            move(iconFile, toIconFile(application, filename))
            filename
        } else {
            null
        }

        return DnsProvider(
            id = id,
            hostname = hostname,
            enabled = enabled,
            icon = iconFilename,
        )
    }

    fun updateDnsProvider(index: Int, hostname: String, iconFile: File? = null) {
        val trimmedHost = hostname.trim()
        if (trimmedHost.isEmpty()) return

        val providers = dnsProviders.toMutableList()
        if (index >= providers.size) return

        viewModelScope.launch {
            val provider = providers[index]

            val currentIconFile = provider.icon?.let { toIconFile(application, it) }
            currentIconFile
                ?.takeIf { it != iconFile }
                ?.let { delete(it) }

            val iconFilename = iconFile?.let {
                val filename = iconFile.name.ifBlank { "${ImageIdGenerator.getNextId(dataStore)}.png" }
                move(it, toIconFile(application, filename))
                filename
            }

            providers[index] = provider.copy(hostname = trimmedHost, icon = iconFilename)
            Timber.d("Updating '%s' to '%s", provider, trimmedHost)

            dataStore.dnsProviders(providers)
        }
    }

    fun deleteDnsProvider(index: Int) {
        val providers = dnsProviders.toMutableList()
        if (index >= providers.size) return

        val provider = providers.removeAt(index)
        Timber.d("Deleting '%s'", provider)

        viewModelScope.launch {
            val newIconFilePath = provider.icon
                ?.let { icon ->
                    // move existing icon to cache dir
                    val currentFile = toIconFile(application, icon)

                    val newFile = File(application.cacheDir, icon)
                    move(currentFile, newFile)

                    newFile.absolutePathIfExists
                }

            dataStore.dnsProviders(providers)
            snackbarMessages.emit(DnsProviderDeleted(index, provider.copy(icon = newIconFilePath)))
        }
    }

    fun restoreDnsProvider(index: Int, deleted: DnsProvider) {
        if (!validateDnsProvider(deleted.hostname)) return

        val providers = dnsProviders.toMutableList()
        viewModelScope.launch {
            val provider = createDnsProvider(
                hostname = deleted.hostname,
                enabled = deleted.enabled,
                iconFile = deleted.icon?.let { File(it) },
            )
            if (index >= providers.size) {
                providers.add(provider)
            } else {
                providers.add(index, provider)
            }
            Timber.d("Restoring '%s' with a new id", deleted)
            dataStore.dnsProviders(providers)
        }
    }

    fun toggleDnsProvider(index: Int, enabled: Boolean) {
        val providers = dnsProviders.toMutableList()
        if (index >= providers.size) return

        val provider = providers[index]
        providers[index] = provider.copy(enabled = enabled)
        Timber.d("Toggling '%s' to %s", provider, enabled)

        viewModelScope.launch {
            dataStore.dnsProviders(providers)
        }
    }

    fun reorderDnsProvider(fromIndex: Int, toIndex: Int) {
        val fromItem = dnsProviders[fromIndex]
        dnsProviders[fromIndex] = dnsProviders[toIndex]
        dnsProviders[toIndex] = fromItem
    }

    fun reorderDnsProviders() {
        viewModelScope.launch { dataStore.dnsProviders(dnsProviders.toList()) }
    }

    fun getSuggestions(text: String): Set<String> {
        val lowercaseText = text.lowercase()
        if (suggestions.contains(lowercaseText)) {
            return emptySet()
        }

        val existingDnsProviders = dnsProviders.asSequence().map { it.hostname.lowercase() }.toSet()
        return suggestions.asSequence()
            .filter { it.contains(lowercaseText) }
            .filterNot { it in existingDnsProviders }
            .toSet()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun backup(dest: Uri) {
        Timber.d("Writing to %s", dest.toString())
        viewModelScope.launch {
            suspendRunCatching {
                val snapshot = SettingsSnapshotV1(
                    dnsOffToggle = dnsOffChecked.value,
                    dnsAutoToggle = dnsAutoChecked.value,
                    requireUnlock = requireUnlockChecked.value,
                    dnsProviders = dnsProviders.map {
                        val iconBase64 = it.icon?.let { icon ->
                            toIconFile(application, icon).toBase64()
                        }

                        DnsProviderSnapshot(
                            hostname = it.hostname,
                            enabled = it.enabled,
                            iconBase64 = iconBase64,
                        )
                    },
                )
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(dest, "wt")?.use { stream ->
                        json.encodeToStream<SettingsSnapshot>(snapshot, stream)
                    }
                }
            }.onSuccess {
                snackbarMessages.emit(BackupCompleted)
            }.onFailure { t ->
                Timber.e(t, "Backup to '%s' failed", dest.toString())
                snackbarMessages.emit(BackupFailed)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun restore(input: Uri) {
        Timber.d("Restoring from %s", input.toString())
        viewModelScope.launch {
            suspendRunCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(input)?.use { stream ->
                        json.decodeFromStream<SettingsSnapshot>(stream)
                    }
                }
            }.map { snapshot ->
                requireNotNull(snapshot) { "Invalid file format" }
            }.onSuccess { snapshot ->
                // delete existing icon files from existing dns
                dnsProviders.asSequence()
                    .mapNotNull { it.icon }
                    .map { toIconFile(application, it) }
                    .forEach { delete(it) }

                when (snapshot) {
                    is SettingsSnapshotV1 -> {
                        dataStore.dnsOffToggle(snapshot.dnsOffToggle)
                        dataStore.dnsAutoToggle(snapshot.dnsAutoToggle)
                        dataStore.requireUnlock(snapshot.requireUnlock)
                        dataStore.dnsProviders(snapshot.dnsProviders
                            .map {
                                // decode base64 icon to a file in the cache dir
                                val imageId = ImageIdGenerator.getNextId(dataStore)
                                val iconFile = it.iconBase64?.let { iconBase64 ->
                                    val file = File(application.cacheDir, "$imageId")
                                    iconBase64.base64DecodeToFile(file)

                                    // process icon file again in case the json was manually edited
                                    // to have a larger/non-image file
                                    processSelectedIcon(file)
                                }

                                createDnsProvider(
                                    hostname = it.hostname,
                                    enabled = it.enabled,
                                    iconFile = iconFile,
                                )
                            })
                    }
                }
                snackbarMessages.emit(RestoreCompleted)
            }.onFailure { t ->
                Timber.e(t, "Restore from backup '%s' failed", input.toString())
                snackbarMessages.emit(RestoreFailed)
            }
        }
    }

    suspend fun processSelectedIcon(input: Uri): File? {
        val imageId = ImageIdGenerator.getNextId(dataStore)
        val src = File(application.cacheDir, "$imageId")
        contentResolver.openInputStream(input)?.use {
            write(it, src)
        }
        return processSelectedIcon(src)
    }

    private suspend fun processSelectedIcon(src: File): File? {
        if (!src.exists()) return null

        val filename = "${src.name}.png"
        val request = ImageRequest.Builder(application)
            .data(src)
            .size(96, 96)
            .build()

        return when (val result = application.imageLoader.execute(request)) {
            is SuccessResult -> {
                val bitmap = result.image.toBitmap()

                val dest = File(application.cacheDir, filename)
                write(bitmap, dest)
                delete(src)
                dest
            }
            is ErrorResult -> {
                Timber.e(result.throwable)
                null
            }
        }
    }

    fun cleanupOrphanImages() {
        val cutoff = System.currentTimeMillis() - 7.days.inWholeMilliseconds

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                application.cacheDir.walkTopDown()
                    .filter {
                        it.isFile &&
                        it.name.endsWith(".png", ignoreCase = true) &&
                        it.lastModified() < cutoff
                    }
                    .forEach { delete(it) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as PrivateDnsApplication
                val savedStateHandle = extras.createSavedStateHandle()
                return MainViewModel(application, savedStateHandle) as T
            }
        }

        private val suggestions = setOf(
            "one.one.one.one",
            "family.cloudflare-dns.com",
            "security.cloudflare-dns.com",
            "dns.google",
            "dns.quad9.net",
            "dns.nextdns.io",
            "dns.adguard-dns.com",
            "family.adguard-dns.com",
            "dns.opendns.com",
            "familyshield.opendns.com",
            "dns.mullvad.net",
            "adblock.dns.mullvad.net",
            "base.dns.mullvad.net",
            "extended.dns.mullvad.net",
            "family.dns.mullvad.net",
            "all.dns.mullvad.net",
        )
    }
}