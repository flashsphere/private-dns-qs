package com.flashsphere.privatednsqs.viewmodel

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import com.flashsphere.privatednsqs.BaseViewModelTest
import com.flashsphere.privatednsqs.util.iconsDir
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AddDnsProviderTest : BaseViewModelTest() {

    @Test
    fun addDnsProvider() = runTest(timeout = 10.seconds) {
        val settingsRepository = createSettingsRepository(backgroundScope)

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        val resIconFile = getFromResources("/icons/icon.png")
        val inputIconFile = File(application.cacheDir, "test-icon.png").apply {
            copy(resIconFile, this)
        }

        viewModel.addDnsProvider("one.one.one.one", inputIconFile)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(1)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].hostname).isEqualTo("one.one.one.one")
            assertThat(dnsProviders[0].enabled).isEqualTo(true)
            assertThat(dnsProviders[0].icon).isEqualTo(inputIconFile.name)

            assertThat(File(application.iconsDir, dnsProviders[0].icon!!).readBytes())
                .isEqualTo(resIconFile.readBytes())

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }

        viewModel.addDnsProvider("dns.google", null)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(2)

            assertThat(dnsProviders[1].id).isGreaterThan(0)
            assertThat(dnsProviders[1].hostname).isEqualTo("dns.google")
            assertThat(dnsProviders[1].enabled).isEqualTo(true)
            assertThat(dnsProviders[1].icon).isNull()

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }

        assertThat(application.iconsDir.listFiles()!!.count()).isEqualTo(1)
        assertThat(application.cacheDir.listFiles()!!.count()).isEqualTo(0)
    }
}
