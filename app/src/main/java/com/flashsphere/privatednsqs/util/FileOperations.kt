package com.flashsphere.privatednsqs.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.encoding.Base64

class FileOperations(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun write(input: InputStream, dest: File): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            mkdirs(dest)
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
            true
        }
        .onFailure { Timber.e(it) }
        .getOrDefault(false)
    }

    suspend fun write(bitmap: Bitmap, dest: File): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            mkdirs(dest)
            dest.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        }
        .onFailure { Timber.e(it) }
        .getOrDefault(false)
    }

    suspend fun move(src: File, dest: File): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            if (!src.exists()) return@suspendRunCatching false
            mkdirs(dest)
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        }
        .onFailure { Timber.e(it) }
        .getOrDefault(false)
    }

    suspend fun delete(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        delete(File(filePath))
    }

    suspend fun delete(file: File) = withContext(ioDispatcher) {
        suspendRunCatching {
            Files.deleteIfExists(file.toPath())
        }
    }

    private suspend fun mkdirs(dest: File) = withContext(ioDispatcher) {
        dest.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    suspend fun toBase64(file: File): String? = withContext(ioDispatcher) {
        if (file.exists() && file.isFile) {
            Base64.encode(file.readBytes())
        } else {
            null
        }
    }

    suspend fun toBitmap(file: File): Bitmap? = withContext(ioDispatcher) {
        file.absolutePathIfExists?.let {
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
            BitmapFactory.decodeFile(it, options)
        }
    }

    suspend fun base64DecodeToFile(base64Encoded: String, dest: File) = withContext(ioDispatcher) {
        val bytes = Base64.decode(base64Encoded)
        dest.writeBytes(bytes)
    }
}

val Context.iconsDir: File
    get() = File(this.filesDir, "icons")

val File.absolutePathIfExists: String?
    get() = if (exists() && isFile) {
        absolutePath
    } else {
        null
    }
