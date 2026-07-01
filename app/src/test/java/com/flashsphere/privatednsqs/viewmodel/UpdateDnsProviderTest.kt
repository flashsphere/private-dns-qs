package com.flashsphere.privatednsqs.viewmodel

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
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
class UpdateDnsProviderTest : BaseViewModelTest() {

    @Test
    fun updateDnsProvider_with_different_hostname_and_no_icon() = runTest(timeout = 10.seconds) {
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
        ))
        runCurrent()

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        viewModel.updateDnsProvider(0, "dns.google", null)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(1)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].hostname).isEqualTo("dns.google")
            assertThat(dnsProviders[0].enabled).isEqualTo(true)
            assertThat(dnsProviders[0].icon).isNull()
            assertThat(currentIcon.exists()).isFalse()

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }
    }

    @Test
    fun updateDnsProvider_with_different_hostname_and_same_icon() = runTest(timeout = 10.seconds) {
        val resIconFile = getFromResources("/icons/icon.png")
        val currentIcon = File(context.iconsDir, "test-icon.png").apply {
            copy(resIconFile, this)
        }

        val settingsRepository = createSettingsRepository(backgroundScope)
        settingsRepository.updateDnsProviders(listOf(
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "one.one.one.one",
                enabled = true,
                icon = currentIcon.name,
            ),
        ))
        runCurrent()

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        viewModel.updateDnsProvider(0, "dns.google", currentIcon)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(1)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].hostname).isEqualTo("dns.google")
            assertThat(dnsProviders[0].enabled).isEqualTo(true)
            assertThat(dnsProviders[0].icon).isEqualTo(currentIcon.name)

            assertThat(File(context.iconsDir, dnsProviders[0].icon!!).readBytes())
                .isEqualTo(resIconFile.readBytes())

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }
    }

    @Test
    fun updateDnsProvider_with_different_icon() = runTest(timeout = 10.seconds) {
        val currentIcon = File(context.iconsDir, "test-icon.png").apply {
            copyFromResources("/icons/icon.png", this)
        }
        val assetNewIcon = getFromResources("/icons/icon2.png")
        val newIcon = File(context.cacheDir, "new-icon.png").apply {
            copy(assetNewIcon, this)
        }

        val settingsRepository = createSettingsRepository(backgroundScope)
        settingsRepository.updateDnsProviders(listOf(
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "one.one.one.one",
                enabled = true,
                icon = currentIcon.name,
            ),
        ))
        runCurrent()

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        viewModel.updateDnsProvider(0, "one.one.one.one", newIcon)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).hasSize(1)

            assertThat(dnsProviders[0].id).isGreaterThan(0)
            assertThat(dnsProviders[0].hostname).isEqualTo("one.one.one.one")
            assertThat(dnsProviders[0].enabled).isEqualTo(true)
            assertThat(dnsProviders[0].icon).isEqualTo(newIcon.name)

            assertThat(File(context.iconsDir, dnsProviders[0].icon!!).readBytes())
                .isEqualTo(assetNewIcon.readBytes())
            assertThat(currentIcon.exists()).isFalse()

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }
    }
}
