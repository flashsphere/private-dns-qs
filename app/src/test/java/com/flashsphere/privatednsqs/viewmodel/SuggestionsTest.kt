package com.flashsphere.privatednsqs.viewmodel

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
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
class SuggestionsTest : BaseViewModelTest() {

    @Test
    fun getSuggestions_return_all_suggestions_if_text_is_empty() = runTest(timeout = 10.seconds) {
        val settingsRepository = createSettingsRepository(backgroundScope)

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        val suggestions = viewModel.getSuggestions("")
        runCurrent()

        assertThat(suggestions).hasSize(suggestions.size)
        assertThat(suggestions).isEqualTo(suggestions)
    }

    @Test
    fun getSuggestions_return_matched_suggestions_based_on_text() = runTest(timeout = 10.seconds) {
        val settingsRepository = createSettingsRepository(backgroundScope)

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        val suggestions = viewModel.getSuggestions("mullvad")
        runCurrent()

        assertThat(suggestions).hasSize(6)
        assertThat(suggestions).isEqualTo(setOf(
            "dns.mullvad.net",
            "adblock.dns.mullvad.net",
            "base.dns.mullvad.net",
            "extended.dns.mullvad.net",
            "family.dns.mullvad.net",
            "all.dns.mullvad.net",
        ))
    }

    @Test
    fun getSuggestions_exclude_items_from_existing_dns_providers() = runTest(timeout = 10.seconds) {
        val settingsRepository = createSettingsRepository(backgroundScope)
        settingsRepository.updateDnsProviders(listOf(
            DnsProvider(
                id = settingsRepository.getNextId(),
                hostname = "dns.mullvad.net",
                enabled = true,
                icon = null,
            ),
        ))
        runCurrent()

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        val suggestions = viewModel.getSuggestions("mullvad")
        runCurrent()

        assertThat(suggestions).hasSize(5)
        assertThat(suggestions).isEqualTo(setOf(
            "adblock.dns.mullvad.net",
            "base.dns.mullvad.net",
            "extended.dns.mullvad.net",
            "family.dns.mullvad.net",
            "all.dns.mullvad.net",
        ))
    }

    @Test
    fun getSuggestions_does_not_return_results_when_text_exactly_matches_suggestions() = runTest(timeout = 10.seconds) {
        val settingsRepository = createSettingsRepository(backgroundScope)

        val viewModel = createViewModel(settingsRepository)
        runCurrent()

        val suggestions = viewModel.getSuggestions("one.one.one.one")
        runCurrent()

        assertThat(suggestions).isEmpty()
    }
}
