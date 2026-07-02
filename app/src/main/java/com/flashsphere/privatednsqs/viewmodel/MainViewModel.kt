package com.flashsphere.privatednsqs.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.privatednsqs.backup.DnsProviderSnapshot
import com.flashsphere.privatednsqs.backup.SettingsSnapshot
import com.flashsphere.privatednsqs.backup.SettingsSnapshotV1
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.datastore.PreferenceKeys
import com.flashsphere.privatednsqs.hilt.IoDispatcher
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.ui.BackupCompleted
import com.flashsphere.privatednsqs.ui.BackupFailed
import com.flashsphere.privatednsqs.ui.DnsProviderDeleted
import com.flashsphere.privatednsqs.ui.RestoreCompleted
import com.flashsphere.privatednsqs.ui.RestoreFailed
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.ImageOperations
import com.flashsphere.privatednsqs.util.PrivateDns
import com.flashsphere.privatednsqs.util.absolutePathIfExists
import com.flashsphere.privatednsqs.util.iconsDir
import com.flashsphere.privatednsqs.util.suspendRunCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.days

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val privateDns: PrivateDns,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileOperations: FileOperations,
    private val imageOperations: ImageOperations,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val contentResolver = context.contentResolver

    val snackbarMessages: SharedFlow<SnackbarMessage>
        field = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.SUSPEND,
        )

    val openHelpDialogFlow: StateFlow<Boolean>
        field = savedStateHandle.getMutableStateFlow("open_help_menu", false)

    val dnsOffStateFlow = settingsRepository.getStateFlow(viewModelScope, PreferenceKeys.DNS_OFF_TOGGLE)
    val dnsAutoStateFlow = settingsRepository.getStateFlow(viewModelScope, PreferenceKeys.DNS_AUTO_TOGGLE)
    val dnsProviders = mutableStateListOf<DnsProvider>()
    val requireUnlockStateFlow = settingsRepository.getStateFlow(viewModelScope, PreferenceKeys.REQUIRE_UNLOCK)
    val showInTileTitleStateFlow = settingsRepository.getStateFlow(viewModelScope, PreferenceKeys.SHOW_IN_TILE_TITLE)

    init {
        settingsRepository.getDnsProvidersFlow()
            .onEach { list ->
                dnsProviders.clear()
                dnsProviders.addAll(list)
            }
            .launchIn(viewModelScope)

        openHelpDialogFlow.value = !hasPermission()
        cleanupOrphanImages()
    }

    fun openHelpDialog(open: Boolean) {
        openHelpDialogFlow.value = open
    }

    fun updateDnsOffToggle(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsOffToggle(checked) }
    }

    fun updateDnsAutoToggle(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateDnsAutoToggle(checked) }
    }

    fun updateRequireUnlock(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateRequireUnlock(checked) }
    }

    fun updateShowInTileTitle(checked: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowInTileTitle(checked) }
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
            settingsRepository.updateDnsProviders(providers)
        }
    }

    private suspend fun createDnsProvider(hostname: String, enabled: Boolean = true, iconFile: File? = null): DnsProvider {
        val id = settingsRepository.getNextId()

        val iconFilename = if (iconFile != null && iconFile.exists()) {
            val filename = iconFile.name.ifBlank { "${settingsRepository.getNextImageId()}.png" }
            fileOperations.move(iconFile, File(context.iconsDir, filename))
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
        if (index >= dnsProviders.size) return

        val trimmedHost = hostname.trim()
        if (trimmedHost.isEmpty()) return

        val providers = dnsProviders.toMutableList()
        viewModelScope.launch {
            val provider = providers[index]

            provider.icon
                ?.let { File(context.iconsDir, it) }
                ?.takeIf { it != iconFile }
                ?.let { fileOperations.delete(it) }

            val iconFilename = iconFile?.let {
                val filename = iconFile.name.ifBlank { "${settingsRepository.getNextImageId()}.png" }
                fileOperations.move(it, File(context.iconsDir, filename))
                filename
            }

            providers[index] = provider.copy(hostname = trimmedHost, icon = iconFilename)
            Timber.d("Updating '%s' to '%s", provider, trimmedHost)

            settingsRepository.updateDnsProviders(providers)
        }
    }

    fun deleteDnsProvider(index: Int) {
        if (index >= dnsProviders.size) return

        val providers = dnsProviders.toMutableList()
        val provider = providers.removeAt(index)
        Timber.d("Deleting '%s'", provider)

        viewModelScope.launch {
            val newIconFilePath = provider.icon
                ?.let { icon ->
                    // move existing icon to cache dir
                    val currentFile = File(context.iconsDir, icon)

                    val newFile = File(context.cacheDir, icon)
                    fileOperations.move(currentFile, newFile)

                    newFile.absolutePathIfExists
                }

            settingsRepository.updateDnsProviders(providers)
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
            settingsRepository.updateDnsProviders(providers)
        }
    }

    fun toggleDnsProvider(index: Int, enabled: Boolean) {
        if (index >= dnsProviders.size) return

        val providers = dnsProviders.toMutableList()
        val provider = providers[index]
        providers[index] = provider.copy(enabled = enabled)
        Timber.d("Toggling '%s' to %s", provider, enabled)

        viewModelScope.launch {
            settingsRepository.updateDnsProviders(providers)
        }
    }

    fun reorderDnsProvider(fromIndex: Int, toIndex: Int) {
        val fromItem = dnsProviders[fromIndex]
        dnsProviders[fromIndex] = dnsProviders[toIndex]
        dnsProviders[toIndex] = fromItem
    }

    fun reorderDnsProviders() {
        val providers = dnsProviders.toList()
        viewModelScope.launch { settingsRepository.updateDnsProviders(providers) }
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
                    dnsOffToggle = dnsOffStateFlow.value,
                    dnsAutoToggle = dnsAutoStateFlow.value,
                    requireUnlock = requireUnlockStateFlow.value,
                    showInTileTitle = showInTileTitleStateFlow.value,
                    dnsProviders = dnsProviders.map {
                        val iconBase64 = it.icon?.let { icon ->
                            fileOperations.toBase64(File(context.iconsDir, icon))
                        }

                        DnsProviderSnapshot(
                            hostname = it.hostname,
                            enabled = it.enabled,
                            iconBase64 = iconBase64,
                        )
                    },
                )
                withContext(ioDispatcher) {
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
                withContext(ioDispatcher) {
                    contentResolver.openInputStream(input)!!.use { stream ->
                        json.decodeFromStream<SettingsSnapshot>(stream)
                    }
                }
            }.onSuccess { snapshot ->
                // delete existing icon files from existing dns
                dnsProviders.asSequence()
                    .mapNotNull { it.icon }
                    .map { File(context.iconsDir, it) }
                    .forEach { fileOperations.delete(it) }

                when (snapshot) {
                    is SettingsSnapshotV1 -> {
                        settingsRepository.updateDnsOffToggle(snapshot.dnsOffToggle)
                        settingsRepository.updateDnsAutoToggle(snapshot.dnsAutoToggle)
                        settingsRepository.updateRequireUnlock(snapshot.requireUnlock)
                        settingsRepository.updateShowInTileTitle(snapshot.showInTileTitle)
                        settingsRepository.updateDnsProviders(snapshot.dnsProviders
                            .map {
                                // decode base64 icon to a file in the cache dir
                                val iconFile = it.iconBase64?.let { iconBase64 ->
                                    val imageId = settingsRepository.getNextImageId()
                                    val file = File(context.cacheDir, "$imageId")
                                    fileOperations.base64DecodeToFile(iconBase64, file)

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
        val imageId = settingsRepository.getNextImageId()
        val src = File(context.cacheDir, "$imageId")
        contentResolver.openInputStream(input)?.use {
            fileOperations.write(it, src)
        }
        return processSelectedIcon(src)
    }

    private suspend fun processSelectedIcon(src: File): File? {
        if (!src.exists()) return null

        return imageOperations.processIcon(src)
            .mapCatching { bitmap ->
                File(context.cacheDir, "${src.name}.png").apply {
                    fileOperations.write(bitmap, this)
                }
            }
            .also { fileOperations.delete(src) }
            .onFailure(Timber::e)
            .getOrNull()
    }

    fun deleteFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return

        viewModelScope.launch {
            fileOperations.delete(filePath)
        }
    }

    fun cleanupOrphanImages() {
        val cutoff = System.currentTimeMillis() - 7.days.inWholeMilliseconds

        viewModelScope.launch {
            withContext(ioDispatcher) {
                context.cacheDir.walkTopDown()
                    .filter {
                        it.isFile &&
                        it.name.endsWith(".png", ignoreCase = true) &&
                        it.lastModified() < cutoff
                    }
                    .forEach { fileOperations.delete(it) }
            }
        }
    }

    companion object {
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