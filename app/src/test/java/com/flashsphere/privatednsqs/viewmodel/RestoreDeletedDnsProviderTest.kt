package com.flashsphere.privatednsqs.viewmodel

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import com.flashsphere.privatednsqs.BaseViewModelTest
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.util.iconsDir
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreDeletedDnsProviderTest : BaseViewModelTest() {

    @Test
    fun restoreDnsProvider() = runTest(timeout = 10.seconds) {
        val resIconFile = getFromResources("/icons/icon.png")
        val iconFile = File(application.cacheDir, "test-icon.png").apply {
            copy(resIconFile, this)
        }

        val settingsRepository = createSettingsRepository(backgroundScope)

        val deletedDnsProvider = DnsProvider(
            id = settingsRepository.getNextId(),
            hostname = "one.one.one.one",
            enabled = true,
            icon = iconFile.absolutePath,
        )
        settingsRepository.updateDnsProviders(listOf(
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

        viewModel.restoreDnsProvider(0, deletedDnsProvider)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(2)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].id).isNotEqualTo(deletedDnsProvider.id)
            assertThat(dnsProviders[0].hostname).isEqualTo("one.one.one.one")
            assertThat(dnsProviders[0].enabled).isEqualTo(true)
            assertThat(dnsProviders[0].icon).isEqualTo("test-icon.png")
            assertThat(File(application.iconsDir, dnsProviders[0].icon!!).readBytes())
                .isEqualTo(resIconFile.readBytes())

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }

        assertThat(application.iconsDir.listFiles()!!.count()).isEqualTo(1)
        assertThat(application.cacheDir.listFiles()!!.count()).isEqualTo(0)
    }
    @Test
    fun restoreDnsProvider_does_not_restore_if_hostname_is_in_list() = runTest(timeout = 10.seconds) {
        val resIconFile = getFromResources("/icons/icon.png")
        val iconFile = File(application.cacheDir, "test-icon.png").apply {
            copy(resIconFile, this)
        }

        val settingsRepository = createSettingsRepository(backgroundScope)

        val deletedDnsProvider = DnsProvider(
            id = settingsRepository.getNextId(),
            hostname = "one.one.one.one",
            enabled = true,
            icon = iconFile.absolutePath,
        )
        settingsRepository.updateDnsProviders(listOf(
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "dns.google",
                enabled = true,
                icon = null,
            ),
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "one.one.one.one",
                enabled = true,
                icon = null,
            ),
        ))
        runCurrent()

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        viewModel.restoreDnsProvider(0, deletedDnsProvider)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(2)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].id).isNotEqualTo(deletedDnsProvider.id)
            assertThat(dnsProviders[0].hostname).isEqualTo("dns.google")
            assertThat(dnsProviders[0].enabled).isEqualTo(true)
            assertThat(dnsProviders[0].icon).isNull()

            assertThat(dnsProviders[1].id).isGreaterThan(0)
            assertThat(dnsProviders[1].id).isNotEqualTo(deletedDnsProvider.id)
            assertThat(dnsProviders[1].hostname).isEqualTo("one.one.one.one")
            assertThat(dnsProviders[1].enabled).isEqualTo(true)
            assertThat(dnsProviders[1].icon).isNull()

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }

        assertThat(application.iconsDir.listFiles()!!.count()).isEqualTo(0)
        assertThat(application.cacheDir.listFiles()!!.count()).isEqualTo(1)
    }
}
