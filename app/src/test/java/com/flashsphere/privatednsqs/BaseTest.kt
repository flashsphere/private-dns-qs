package com.flashsphere.privatednsqs

import org.junit.Before
import org.junit.Rule
import java.io.File
import kotlin.io.path.createTempDirectory

abstract class BaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    val testDispatcher = mainDispatcherRule.testDispatcher

    lateinit var tempDir: File

    @Before
    fun baseTestSetup() {
        tempDir = createTempDirectory().toFile()
    }

    fun copyFromResources(src: String, dest: File) {
        copy(getFromResources(src), dest)
    }

    fun copy(src: File, dest: File) {
        src.inputStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun getFromResources(path: String): File {
        return File(javaClass.getResource(path)!!.toURI())
    }
}