package com.flashsphere.privatednsqs.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.privatednsqs.BaseTest
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.DnsConfiguration
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.iconsDir
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

class DefaultTileInfoUpdaterTest : BaseTest() {
    lateinit var context: Context
    lateinit var fileOperations: FileOperations
    lateinit var settingsRepository: SettingsRepository
    lateinit var tileInfoUpdater: TileInfoUpdater

    @Before
    fun setup() {
        mockkStatic(Icon::class)

        context = mockk<Context>().also {
            every { it.cacheDir } returns File(tempDir, "cache").apply { mkdirs() }
            every { it.filesDir } returns File(tempDir, "data").apply { mkdirs() }
            it.iconsDir.mkdirs()

            every { it.getString(R.string.off) } returns "Off"
            every { it.getString(R.string.auto) } returns "Auto"
            every { it.getString(R.string.on) } returns "On"
            every { it.getString(R.string.tile_name) } returns "Tile name"
        }
        fileOperations = mockk()
        settingsRepository = mockk()
        tileInfoUpdater = DefaultTileInfoUpdater(context, fileOperations, settingsRepository)
    }

    @After
    fun tearDown() {
        Build.VERSION.reset()
    }

    @Test
    fun update_with_dns_off() = runTest(timeout = 10.seconds) {
        coEvery { settingsRepository.getShowInTileTitle() } returns false
        coEvery { settingsRepository.getDnsAutoAsInactiveTile() } returns false

        val resourceCapture = slot<Int>()
        val icon = mockk<Icon>()
        every { Icon.createWithResource(context, capture(resourceCapture)) } returns icon

        val tile = mockk<Tile>(relaxed = true)
        val dnsConfig = DnsConfiguration.Off
        tileInfoUpdater.update(tile, dnsConfig)

        verify {
            tile.state = Tile.STATE_INACTIVE
            tile.stateDescription = "Off"
            tile.label = "Tile name"
            tile.subtitle = "Off"
            tile.icon = icon
            tile.contentDescription = "Tile name"
            tile.updateTile()
        }
        confirmVerified(tile)
        assertThat(resourceCapture.captured).isEqualTo(dnsConfig.mode.iconResId)
    }

    @Test
    fun update_with_dns_off_and_api_below_android_10() = runTest(timeout = 10.seconds) {
        Build.VERSION.SDK_INT = 28
        coEvery { settingsRepository.getShowInTileTitle() } returns false
        coEvery { settingsRepository.getDnsAutoAsInactiveTile() } returns false

        val resourceCapture = slot<Int>()
        val icon = mockk<Icon>()
        every { Icon.createWithResource(context, capture(resourceCapture)) } returns icon

        val tile = mockk<Tile>(relaxed = true)
        val dnsConfig = DnsConfiguration.Off
        tileInfoUpdater.update(tile, dnsConfig)

        verify {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Off"
            tile.icon = icon
            tile.contentDescription = "Tile name"
            tile.updateTile()
        }
        confirmVerified(tile)
        assertThat(resourceCapture.captured).isEqualTo(dnsConfig.mode.iconResId)
    }

    @Test
    fun update_with_dns_auto() = runTest(timeout = 10.seconds) {
        coEvery { settingsRepository.getShowInTileTitle() } returns false
        coEvery { settingsRepository.getDnsAutoAsInactiveTile() } returns false

        val resourceCapture = slot<Int>()
        val icon = mockk<Icon>()
        every { Icon.createWithResource(context, capture(resourceCapture)) } returns icon

        val tile = mockk<Tile>(relaxed = true)
        val dnsConfig = DnsConfiguration.Auto
        tileInfoUpdater.update(tile, dnsConfig)

        verify {
            tile.state = Tile.STATE_ACTIVE
            tile.stateDescription = "Auto"
            tile.label = "Tile name"
            tile.subtitle = "Auto"
            tile.icon = icon
            tile.contentDescription = "Tile name"
            tile.updateTile()
        }
        confirmVerified(tile)
        assertThat(resourceCapture.captured).isEqualTo(dnsConfig.mode.iconResId)
    }

    @Test
    fun update_with_dns_auto_and_as_inactive_tile() = runTest(timeout = 10.seconds) {
        coEvery { settingsRepository.getShowInTileTitle() } returns false
        coEvery { settingsRepository.getDnsAutoAsInactiveTile() } returns true

        val resourceCapture = slot<Int>()
        val icon = mockk<Icon>()
        every { Icon.createWithResource(context, capture(resourceCapture)) } returns icon

        val tile = mockk<Tile>(relaxed = true)
        val dnsConfig = DnsConfiguration.Auto
        tileInfoUpdater.update(tile, dnsConfig)

        verify {
            tile.state = Tile.STATE_INACTIVE
            tile.stateDescription = "Auto"
            tile.label = "Tile name"
            tile.subtitle = "Auto"
            tile.icon = icon
            tile.contentDescription = "Tile name"
            tile.updateTile()
        }
        confirmVerified(tile)
        assertThat(resourceCapture.captured).isEqualTo(dnsConfig.mode.iconResId)
    }

    @Test
    fun update_with_dns_on() = runTest(timeout = 10.seconds) {
        coEvery { settingsRepository.getShowInTileTitle() } returns false
        coEvery { settingsRepository.getDnsAutoAsInactiveTile() } returns false

        val bitmapCapture = slot<Bitmap>()
        val icon = mockk<Icon>()
        every { Icon.createWithBitmap(capture(bitmapCapture)) } returns icon
        val iconFile = File(context.iconsDir, "test-icon.png")

        val bitmap = mockk<Bitmap>()
        coEvery { fileOperations.toBitmap(iconFile) } returns bitmap

        val tile = mockk<Tile>(relaxed = true)
        val dnsConfig = DnsConfiguration.On("one.one.one.one", "test-icon.png")
        tileInfoUpdater.update(tile, dnsConfig)

        verify {
            tile.state = Tile.STATE_ACTIVE
            tile.stateDescription = "one.one.one.one"
            tile.label = "Tile name"
            tile.subtitle = "one.one.one.one"
            tile.icon = icon
            tile.contentDescription = "Tile name"
            tile.updateTile()
        }
        confirmVerified(tile)
        coVerify { fileOperations.toBitmap(iconFile) }
        assertThat(bitmapCapture.captured).isEqualTo(bitmap)
    }

    @Test
    fun update_with_dns_on_and_show_in_tile_title() = runTest(timeout = 10.seconds) {
        coEvery { settingsRepository.getShowInTileTitle() } returns true
        coEvery { settingsRepository.getDnsAutoAsInactiveTile() } returns false

        val bitmapCapture = slot<Bitmap>()
        val icon = mockk<Icon>()
        every { Icon.createWithBitmap(capture(bitmapCapture)) } returns icon
        val iconFile = File(context.iconsDir, "test-icon.png")

        val bitmap = mockk<Bitmap>()
        coEvery { fileOperations.toBitmap(iconFile) } returns bitmap

        val tile = mockk<Tile>(relaxed = true)
        val dnsConfig = DnsConfiguration.On("one.one.one.one", "test-icon.png")
        tileInfoUpdater.update(tile, dnsConfig)

        verify {
            tile.state = Tile.STATE_ACTIVE
            tile.stateDescription = "one.one.one.one"
            tile.label = "one.one.one.one"
            tile.subtitle = null
            tile.icon = icon
            tile.contentDescription = "Tile name"
            tile.updateTile()
        }
        confirmVerified(tile)
        coVerify { fileOperations.toBitmap(iconFile) }
        assertThat(bitmapCapture.captured).isEqualTo(bitmap)
    }
}