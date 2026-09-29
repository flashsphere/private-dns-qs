package com.flashsphere.privatednsqs.shortcut

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.fail
import com.flashsphere.privatednsqs.BaseTest
import com.flashsphere.privatednsqs.BuildConfig
import com.flashsphere.privatednsqs.datastore.DnsProvider
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.LauncherIconManager
import com.flashsphere.privatednsqs.util.iconsDir
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutManagerTest : BaseTest() {
    lateinit var context: Context
    lateinit var fileOperations: FileOperations
    lateinit var settingsRepository: SettingsRepository
    lateinit var launcherIconManager: LauncherIconManager
    lateinit var shortcutIdBuilder: ShortcutIdBuilder
    lateinit var shortcutBuilder: ShortcutBuilder
    lateinit var shortcutManager: ShortcutManager

    @Before
    fun setup() {
        mockkStatic(ShortcutManagerCompat::class)
        every { ShortcutManagerCompat.isRequestPinShortcutSupported(any()) } returns true
        every { ShortcutManagerCompat.setDynamicShortcuts(any(), any()) } returns true
        every { ShortcutManagerCompat.updateShortcuts(any(), any()) } returns true
        every { ShortcutManagerCompat.requestPinShortcut(any(), any(), any()) } returns true
        every { ShortcutManagerCompat.disableShortcuts(any(), any(), any()) } just Runs

        context = mockk<Context>().also {
            every { it.cacheDir } returns File(tempDir, "cache").apply { mkdirs() }
            every { it.filesDir } returns File(tempDir, "data").apply { mkdirs() }
            every { it.getString(any()) } answers { "String res id: ${arg<Int>(0)}" }
            it.iconsDir.mkdirs()
        }
        fileOperations = mockk()
        settingsRepository = mockk()
        launcherIconManager = mockk<LauncherIconManager>().also {
            every { it.iconVisibleFlow } returns MutableStateFlow(true)
        }
        shortcutBuilder = mockk<ShortcutBuilder>().also {
            every { it.buildDnsOffShortcut(any()) } returns mockk()
            every { it.buildDnsAutoShortcut(any()) } returns mockk()
            every { it.buildDnsToggleShortcut(any()) } returns mockk()
            coEvery { it.buildProviderShortcut(any(), any()) } returns mockk()
        }
        shortcutIdBuilder = ShortcutIdBuilderImpl()
        shortcutManager = ShortcutManager(context, settingsRepository, launcherIconManager,
            shortcutIdBuilder, shortcutBuilder)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun count_shortcuts() {
        val shortcutCount = shortcutManager.countShortcuts(
            dnsProviders = listOf(
                DnsProvider(
                    id = 1,
                    hostname = "one.one.one.one",
                    enabled = true,
                    shortcutEnabled = true,
                    icon = null,
                ),
                DnsProvider(
                    id = 2,
                    hostname = "dns.google",
                    enabled = true,
                    shortcutEnabled = true,
                    icon = null,
                ),
            ),
            showOff = true,
            showAuto = true,
            showToggle = true,
        )

        assertThat(shortcutCount).isEqualTo(5)
    }

    @Test
    fun update_shortcuts_with_more_than_4_shortcuts() = runTest(timeout = 10.seconds) {
        every { ShortcutManagerCompat.getShortcuts(any(), any()) } returns emptyList()

        shortcutManager.updateShortcuts(
            dnsProviders = listOf(
                DnsProvider(
                    id = 1,
                    hostname = "one.one.one.one",
                    enabled = true,
                    shortcutEnabled = true,
                    icon = null,
                ),
                DnsProvider(
                    id = 2,
                    hostname = "dns.google",
                    enabled = true,
                    shortcutEnabled = true,
                    icon = null,
                ),
            ),
            showOff = true,
            showAuto = true,
            showToggle = true,
        )
        runCurrent()

        verify(exactly = 1) {
            ShortcutManagerCompat.getShortcuts(context, any())
            ShortcutManagerCompat.setDynamicShortcuts(
                context,
                withArg {
                    assertThat(it).hasSize(4)
                })
            ShortcutManagerCompat.updateShortcuts(
                context,
                withArg {
                    assertThat(it).isEmpty()
                })
            ShortcutManagerCompat.disableShortcuts(context, any(), any())
        }
    }

    @Test
    fun update_shortcuts_with_existing_pinned_shortcuts() = runTest(timeout = 10.seconds) {
        val dnsProviders = listOf(
            DnsProvider(
                id = 1,
                hostname = "one.one.one.one",
                enabled = true,
                shortcutEnabled = true,
                icon = null,
            ),
            DnsProvider(
                id = 2,
                hostname = "dns.google",
                enabled = true,
                shortcutEnabled = true,
                icon = null,
            ),
        )

        val existingPinnedShortcuts = listOf(
            mockk<ShortcutInfoCompat>().also {
                every { it.id } returns shortcutIdBuilder.buildDnsOffPinnedShortcutId()
            },
            mockk<ShortcutInfoCompat>().also {
                every { it.id } returns shortcutIdBuilder.buildDnsProviderPinnedShortcutId(dnsProviders[0])
            },
            mockk<ShortcutInfoCompat>().also {
                every { it.id } returns shortcutIdBuilder.buildDnsProviderPinnedShortcutId(DnsProvider(
                    id = 3,
                    hostname = "dns.quad9.net",
                    enabled = true,
                    shortcutEnabled = true,
                    icon = null,
                ))
            },
        )
        every { ShortcutManagerCompat.getShortcuts(any(), FLAG_MATCH_PINNED) } returns existingPinnedShortcuts

        shortcutManager.updateShortcuts(
            dnsProviders = dnsProviders,
            showOff = true,
            showAuto = true,
            showToggle = true,
        )
        runCurrent()

        verify(exactly = 1) {
            ShortcutManagerCompat.getShortcuts(context, any())
            ShortcutManagerCompat.setDynamicShortcuts(
                context,
                withArg {
                    assertThat(it).hasSize(4)
                })
            ShortcutManagerCompat.updateShortcuts(
                context,
                withArg {
                    assertThat(it).hasSize(2)
                })
            ShortcutManagerCompat.disableShortcuts(
                context,
                withArg {
                    assertThat(it).hasSize(1)
                },
                any())
        }
    }

    @Test
    fun update_shortcut() = runTest(timeout = 10.seconds) {
        shortcutManager.updateShortcut(DnsProvider(
            id = 1,
            hostname = "one.one.one.one",
            enabled = true,
            shortcutEnabled = true,
            icon = null,
        ))
        runCurrent()

        verify(exactly = 1) {
            ShortcutManagerCompat.updateShortcuts(
                context,
                withArg {
                    when (BuildConfig.FLAVOR) {
                        "launcher" -> assertThat(it).hasSize(1)
                        "nolauncher" -> assertThat(it).hasSize(2)
                        else -> fail("Unexpected build variant: ${BuildConfig.FLAVOR}")
                    }
                })
        }
    }

    @Test
    fun disable_shortcut() {
        shortcutManager.disableShortcut(DnsProvider(
            id = 1,
            hostname = "one.one.one.one",
            enabled = true,
            shortcutEnabled = true,
            icon = null,
        ))

        verify(exactly = 1) {
            ShortcutManagerCompat.disableShortcuts(
                context,
                withArg {
                    assertThat(it).hasSize(1)
                },
                any())
        }
    }
}