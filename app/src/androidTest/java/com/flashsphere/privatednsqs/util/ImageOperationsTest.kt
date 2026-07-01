package com.flashsphere.privatednsqs.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ImageOperationsTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var imageOperations: ImageOperations
    lateinit var testContext: Context
    lateinit var tempDir: File

    @Before
    fun setup() {
        hiltRule.inject()
        testContext = InstrumentationRegistry.getInstrumentation().context
        tempDir = File(context.cacheDir, "temp").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun process_icon_from_svg() = runTest(timeout = 15.seconds) {
        val inputIconFile = File(tempDir, "icon.svg").apply {
            copy("icons/icon.svg", this)
        }

        val result = imageOperations.processIcon(inputIconFile)

        assertThat(result).isSuccess()
        assertBitmap(result.getOrThrow(), "icons/icon.png")
    }

    @Test
    fun process_icon_from_png() = runTest(timeout = 15.seconds) {
        val inputIconFile = File(tempDir, "icon.png").apply {
            copy("icons/icon.png", this)
        }

        val result = imageOperations.processIcon(inputIconFile)

        assertThat(result).isSuccess()
        assertBitmap(result.getOrThrow(), "icons/icon.png")
    }

    private fun assertBitmap(actual: Bitmap, expected: String) {
        val expectedBitmap = testContext.assets.open(expected).use {
            val bitmapOptions = BitmapFactory.Options().apply { inPreferredConfig = actual.config }
            BitmapFactory.decodeStream(it, null, bitmapOptions)
        }

        assertThat(actual.sameAs(expectedBitmap)).isTrue()
    }

    private fun copy(assetPath: String, dest: File) {
        testContext.assets.open(assetPath).use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
