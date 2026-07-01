package com.flashsphere.privatednsqs.util

import android.content.Context
import coil3.Bitmap
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File

@Singleton
class ImageOperations @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {
    suspend fun processIcon(src: File): Result<Bitmap> {
        val request = ImageRequest.Builder(context)
            .data(src)
            .size(ICON_SIZE)
            .build()

        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> {
                Result.success(result.image.toBitmap())
            }
            is ErrorResult -> {
                Result.failure(result.throwable)
            }
        }
    }

    companion object {
        private const val ICON_SIZE = 96
    }
}
