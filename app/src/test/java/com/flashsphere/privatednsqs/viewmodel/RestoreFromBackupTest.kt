package com.flashsphere.privatednsqs.viewmodel

import android.net.Uri
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.flashsphere.privatednsqs.BaseViewModelTest
import com.flashsphere.privatednsqs.ui.RestoreCompleted
import com.flashsphere.privatednsqs.util.iconsDir
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreFromBackupTest : BaseViewModelTest() {

    @Test
    fun restore() = runTest(timeout = 10.seconds) {
        val settingsRepository = createSettingsRepository(backgroundScope)
        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        val deferred = async {
            viewModel.snackbarMessages.first()
        }

        val inputFile = File(javaClass.getResource("/backup/backup.txt")!!.toURI())
        val input = mockk<Uri>().also {
            every { it.toString() } answers { inputFile.toString() }
        }

        viewModel.restore(input)
        runCurrent()

        assertThat(deferred.getCompleted()).isEqualTo(RestoreCompleted)

        assertThat(settingsRepository.getDnsOffToggle()).isEqualTo(false)
        assertThat(settingsRepository.getDnsAutoToggle()).isEqualTo(true)
        assertThat(settingsRepository.getRequireUnlock()).isEqualTo(true)

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(2)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].hostname).isEqualTo("one.one.one.one")
            assertThat(dnsProviders[0].enabled).isEqualTo(true)
            assertThat(dnsProviders[0].icon).isNotNull().endsWith(".png")
            assertThat(File(application.iconsDir, dnsProviders[0].icon!!).readBytes())
                .isEqualTo(getFromResources("/icons/icon.png").readBytes())

            assertThat(dnsProviders[1].id).isGreaterThan(0)
            assertThat(dnsProviders[1].hostname).isEqualTo("dns.google")
            assertThat(dnsProviders[1].enabled).isEqualTo(false)
            assertThat(dnsProviders[1].icon).isNull()

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }

        assertThat(application.iconsDir.listFiles()!!.count()).isEqualTo(1)
        assertThat(application.cacheDir.listFiles()!!.count()).isEqualTo(0)

        coVerify(exactly = 1) { imageOperations.processIcon(any()) }
    }
}
