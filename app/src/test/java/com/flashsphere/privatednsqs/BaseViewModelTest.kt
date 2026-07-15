package com.flashsphere.privatednsqs

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.ImageOperations
import com.flashsphere.privatednsqs.util.PrivateDns
import com.flashsphere.privatednsqs.util.iconsDir
import com.flashsphere.privatednsqs.viewmodel.MainViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
abstract class BaseViewModelTest : BaseTest() {
    lateinit var context: Context
    lateinit var privateDns: PrivateDns
    lateinit var contentResolver: ContentResolver
    lateinit var imageOperations: ImageOperations
    lateinit var json: Json

    @Before
    fun baseVmTestSetup() {
        imageOperations = mockk<ImageOperations>().also {
            coEvery { it.processIcon(any()) } answers {
                val src = arg<File>(0)
                val bitmap = mockk<Bitmap>().also { bitmap ->
                    every { bitmap.compress(any(), any(), any()) } answers {
                        val outputStream = arg<OutputStream>(2)
                        src.inputStream().use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        true
                    }
                }
                Result.success(bitmap)
            }
        }

        contentResolver = mockk<ContentResolver>().also {
            every { it.openOutputStream(any(), any()) } answers {
                val uri = arg<Uri>(0)
                FileOutputStream(uri.toString())
            }
            every { it.openInputStream(any()) } answers {
                val uri = arg<Uri>(0)
                FileInputStream(uri.toString())
            }
        }

        privateDns = mockk<PrivateDns>().also {
            every { it.hasPermission() } returns false
        }

        context = mockk<Context>().also {
            every { it.cacheDir } returns File(tempDir, "cache").apply { mkdirs() }
            every { it.filesDir } returns File(tempDir, "data").apply { mkdirs() }
            it.iconsDir.mkdirs()
            every { it.contentResolver } returns contentResolver
        }

        json = Json {
            ignoreUnknownKeys = true
            exceptionsWithDebugInfo = true
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    fun createSettingsRepository(scope: CoroutineScope): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(context.filesDir, "settings.preferences_pb") }
        )
        return SettingsRepository(
            dataStore = dataStore,
            json = json,
            computeDispatcher = testDispatcher,
        )
    }

    fun createViewModel(settingsRepository: SettingsRepository): MainViewModel {
        return MainViewModel(
            context = context,
            json = json,
            privateDns = privateDns,
            ioDispatcher = testDispatcher,
            fileOperations = FileOperations(testDispatcher),
            imageOperations = imageOperations,
            settingsRepository = settingsRepository,
            savedStateHandle = SavedStateHandle(),
        )
    }
}
