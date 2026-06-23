package com.flashsphere.privatednsqs.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object FileUtils {
    suspend fun write(input: InputStream, dest: File): Boolean = withContext(Dispatchers.IO) {
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

    suspend fun write(bitmap: Bitmap, dest: File): Boolean = withContext(Dispatchers.IO) {
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

    suspend fun move(src: File, dest: File): Boolean = withContext(Dispatchers.IO) {
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

    suspend fun delete(file: File) = withContext(Dispatchers.IO) {
        suspendRunCatching {
            Files.deleteIfExists(file.toPath())
        }
    }

    private suspend fun mkdirs(dest: File) = withContext(Dispatchers.IO) {
        dest.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun getIconsDir(context: Context): File {
        return File(context.filesDir, "icons")
    }

    fun toIconFile(context: Context, iconFilename: String): File {
        return File(getIconsDir(context), iconFilename)
    }
}

val File.absolutePathIfExists: String?
    get() = if (exists() && isFile) {
        absolutePath
    } else {
        null
    }

suspend fun File.toBase64(): String? = withContext(Dispatchers.IO) {
    if (exists() && isFile) {
        Base64.encodeToString(readBytes(), Base64.NO_WRAP)
    } else {
        null
    }
}

suspend fun File.toBitmap(): Bitmap? = withContext(Dispatchers.IO) {
    absolutePathIfExists?.let { BitmapFactory.decodeFile(it) }
}

suspend fun String.base64DecodeToFile(dest: File) = withContext(Dispatchers.IO) {
    val bytes = Base64.decode(this@base64DecodeToFile, Base64.DEFAULT)
    dest.writeBytes(bytes)
}
