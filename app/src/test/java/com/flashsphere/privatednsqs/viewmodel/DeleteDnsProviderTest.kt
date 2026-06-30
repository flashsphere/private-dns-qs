package com.flashsphere.privatednsqs.viewmodel

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.flashsphere.privatednsqs.BaseViewModelTest
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.ui.DnsProviderDeleted
import com.flashsphere.privatednsqs.util.iconsDir
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteDnsProviderTest : BaseViewModelTest() {

    @Test
    fun deleteDnsProvider() = runTest(timeout = 10.seconds) {
        val currentIcon = File(application.iconsDir, "test-icon.png").apply {
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

        val deferred = async {
            viewModel.snackbarMessages.first()
        }

        viewModel.deleteDnsProvider(0)
        runCurrent()

        settingsRepository.getDnsProviders().let { dnsProviders ->
            assertThat(dnsProviders).isEmpty()
            assertThat(currentIcon.exists()).isFalse()

            assertThat(viewModel.dnsProviders.toList()).isEqualTo(dnsProviders)
        }

        val message = deferred.getCompleted()
        assertThat(message).isInstanceOf<DnsProviderDeleted>()

        message as DnsProviderDeleted
        message.dnsProvider.let { dnsProvider ->
            assertThat(dnsProvider.id).isGreaterThan(0)
            assertThat(dnsProvider.hostname).isEqualTo("one.one.one.one")
            assertThat(dnsProvider.enabled).isEqualTo(true)
            assertThat(dnsProvider.icon).isNotNull().endsWith(".png")
            assertThat(File(dnsProvider.icon!!).readBytes())
                .isEqualTo(getFromResources("/icons/icon.png").readBytes())
        }

        assertThat(application.iconsDir.listFiles()!!.count()).isEqualTo(0)
        assertThat(application.cacheDir.listFiles()!!.count()).isEqualTo(1)
    }
}
