package com.flashsphere.privatednsqs.viewmodel

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import com.flashsphere.privatednsqs.BaseViewModelTest
import com.flashsphere.privatednsqs.util.iconsDir
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupOrphanImagesTest : BaseViewModelTest() {

    @Test
    fun cleanupOrphanImages() = runTest(timeout = 10.seconds) {
        val settingsRepository = createSettingsRepository(backgroundScope)

        val cutoff = System.currentTimeMillis() - 7.days.inWholeMilliseconds

        val resIconFile = getFromResources("/icons/icon.png")
        val iconFile = File(application.iconsDir, "test-icon.png").apply {
            copy(resIconFile, this)
            this.setLastModified(cutoff - 1.hours.inWholeMilliseconds)
        }
        val iconFile1 = File(application.cacheDir, "test-icon1.png").apply {
            copy(resIconFile, this)
            this.setLastModified(cutoff - 1.hours.inWholeMilliseconds)
        }
        val iconFile2 = File(application.cacheDir, "test-icon2.png").apply {
            copy(resIconFile, this)
            this.setLastModified(cutoff + 1.hours.inWholeMilliseconds)
        }
        val iconFile3 = File(application.cacheDir, "test-icon3.png").apply {
            copy(resIconFile, this)
            this.setLastModified(cutoff + 1.hours.inWholeMilliseconds)
        }

        createViewModel(settingsRepository)
        runCurrent()

        assertThat(iconFile).exists()
        assertThat(iconFile1.exists()).isFalse()
        assertThat(iconFile2).exists()
        assertThat(iconFile3).exists()

        assertThat(application.iconsDir.listFiles()!!.count()).isEqualTo(1)
        assertThat(application.cacheDir.listFiles()!!.count()).isEqualTo(2)
    }
}
