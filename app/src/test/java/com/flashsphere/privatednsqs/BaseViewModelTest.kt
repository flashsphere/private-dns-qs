package com.flashsphere.privatednsqs

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.flashsphere.privatednsqs.repository.SettingsRepository
import com.flashsphere.privatednsqs.util.FileOperations
import com.flashsphere.privatednsqs.util.ImageOperations
import com.flashsphere.privatednsqs.util.iconsDir
import com.flashsphere.privatednsqs.viewmodel.MainViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalSerializationApi::class)
abstract class BaseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    val testDispatcher = mainDispatcherRule.testDispatcher
    lateinit var tempDir: File
    lateinit var application: PrivateDnsApplication
    lateinit var contentResolver: ContentResolver
    lateinit var imageOperations: ImageOperations
    lateinit var json: Json

    @Before
    fun setup() {
        tempDir = createTempDirectory().toFile()

        mockkStatic(ContextCompat::class).also {
            every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED
        }

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

        application = mockk<PrivateDnsApplication>().also {
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

    fun copyFromResources(asset: String, dest: File) {
        copy(getFromResources(asset), dest)
    }

    fun copy(asset: File, dest: File) {
        asset.inputStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun getFromResources(asset: String): File {
        return File(javaClass.getResource(asset)!!.toURI())
    }

    fun createSettingsRepository(scope: CoroutineScope): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(application.filesDir, "settings.preferences_pb") }
        )
        return SettingsRepository(
            dataStore = dataStore,
            json = json,
            computeDispatcher = testDispatcher,
        )
    }

    fun createViewModel(settingsRepository: SettingsRepository): MainViewModel {
        return MainViewModel(
            application = application,
            json = json,
            ioDispatcher = testDispatcher,
            fileOperations = FileOperations(testDispatcher),
            imageOperations = imageOperations,
            settingsRepository = settingsRepository,
            savedStateHandle = SavedStateHandle(),
        )
    }
}