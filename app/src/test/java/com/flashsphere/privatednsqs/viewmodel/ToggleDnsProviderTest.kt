package com.flashsphere.privatednsqs.viewmodel

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.flashsphere.privatednsqs.BaseViewModelTest
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.util.iconsDir
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ToggleDnsProviderTest : BaseViewModelTest() {

    @Test
    fun toggleDnsProvider() = runTest(timeout = 10.seconds) {
        val currentIcon = File(context.iconsDir, "test-icon.png").apply {
            copyFromResources("/icons/icon.png", this)
        }

        val settingsRepository = createSettingsRepository(backgroundScope)
        settingsRepository.updateDnsProviders(listOf(
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "one.one.one.one",
                enabled = true,
                icon = currentIcon.name,
            ),
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "dns.google",
                enabled = true,
                icon = null,
            ),
        ))
        runCurrent()

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        viewModel.toggleDnsProvider(0, false)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(2)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].hostname).isEqualTo("one.one.one.one")
            assertThat(dnsProviders[0].enabled).isEqualTo(false)
            assertThat(dnsProviders[0].icon).isNotNull()

            assertThat(dnsProviders[1].id).isGreaterThan(0)
            assertThat(dnsProviders[1].hostname).isEqualTo("dns.google")
            assertThat(dnsProviders[1].enabled).isEqualTo(true)
            assertThat(dnsProviders[1].icon).isNull()

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }

        assertThat(context.iconsDir.listFiles()!!.count()).isEqualTo(1)
        assertThat(context.cacheDir.listFiles()!!.count()).isEqualTo(0)
    }
}
