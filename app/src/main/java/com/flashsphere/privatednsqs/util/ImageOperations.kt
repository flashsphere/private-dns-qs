package com.flashsphere.privatednsqs.util

import android.content.Context
import coil3.Bitmap
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.size.Precision
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
    private val transformations = listOf(NormalizeIconTransformation())

    suspend fun processIcon(src: File): Result<Bitmap> {
        val request = ImageRequest.Builder(context)
            .data(src)
            .size(MAX_DECODE_SIZE_PX) // decode large images at a bounded size
            .precision(Precision.INEXACT) // do not upscale if is smaller size
            .transformations(transformations)
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
        private const val MAX_DECODE_SIZE_PX = 108
    }
}
