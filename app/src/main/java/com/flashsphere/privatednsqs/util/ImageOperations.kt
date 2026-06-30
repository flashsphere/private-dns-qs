package com.flashsphere.privatednsqs.util

import coil3.Bitmap
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.flashsphere.privatednsqs.PrivateDnsApplication
import java.io.File

class ImageOperations(
    private val application: PrivateDnsApplication,
) {
    private val imageLoader = application.imageLoader

    suspend fun processIcon(src: File): Result<Bitmap> {
        val request = ImageRequest.Builder(application)
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
