package com.flashsphere.privatednsqs.viewmodel

import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.privatednsqs.BaseViewModelTest
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.ui.BackupCompleted
import com.flashsphere.privatednsqs.util.iconsDir
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BackupTest : BaseViewModelTest() {

    @Test
    fun backup() = runTest(timeout = 10.seconds) {
        val existingIconFile = File(application.iconsDir, "test-icon.png").apply {
            copyFromResources("/icons/icon.png", this)
        }

        val settingsRepository = createSettingsRepository(backgroundScope)
        settingsRepository.updateDnsOffToggle(false)
        settingsRepository.updateDnsAutoToggle(true)
        settingsRepository.updateRequireUnlock(true)
        settingsRepository.updateDnsProviders(listOf(
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "one.one.one.one",
                enabled = true,
                icon = existingIconFile.name,
            ),
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "dns.google",
                enabled = false,
                icon = null,
            )
        ))
        runCurrent()

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        val deferred = async { viewModel.snackbarMessages.first() }

        val outputFile = File(tempDir, "backup.txt")
        val dest = mockk<Uri>().also {
            every { it.toString() } answers { outputFile.toString() }
        }

        viewModel.backup(dest)
        runCurrent()

        assertThat(deferred.getCompleted()).isEqualTo(BackupCompleted)

        val expectedJson = javaClass.getResourceAsStream("/backup/backup.txt")!!
            .bufferedReader()
            .readText()
        JSONAssert.assertEquals(
            expectedJson,
            outputFile.readText(),
            JSONCompareMode.STRICT,
        )

        assertThat(application.iconsDir.listFiles()!!.count()).isEqualTo(1)
        assertThat(application.cacheDir.listFiles()!!.count()).isEqualTo(0)
    }
}
