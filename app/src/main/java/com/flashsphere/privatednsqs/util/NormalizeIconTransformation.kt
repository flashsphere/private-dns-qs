package com.flashsphere.privatednsqs.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.transform.Transformation
import timber.log.Timber

class NormalizeIconTransformation : Transformation() {
    override val cacheKey = "NormalizeIconTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Get bounding rectangle of all non-transparent pixels,
        // null if the bitmap contains no visible content.
        val src = getContentBounds(input)
        if (src == null) {
            Timber.d("No transformation done")
            return input
        }

        val contentWidth = src.width()
        val contentHeight = src.height()
        Timber.d("content size: %d x %d", contentWidth, contentHeight)

        // scale content size area, so that the padding at the sides are consistent
        // in the result bitmap
        val scale = CONTENT_SIZE_PX.toFloat() / maxOf(contentWidth, contentHeight)
        val scaledWidth = contentWidth * scale
        val scaledHeight = contentHeight * scale
        Timber.d("scaled size: %.2f x %.2f", scaledWidth, scaledHeight)

        val result = createBitmap(ICON_SIZE_PX, ICON_SIZE_PX)

        val left = (ICON_SIZE_PX - scaledWidth) / 2f
        val top = (ICON_SIZE_PX - scaledHeight) / 2f
        val dst = RectF(left, top, left + scaledWidth, top + scaledHeight)

        val paint = Paint().apply {
            isFilterBitmap = true
        }
        Canvas(result).drawBitmap(input, src, dst, paint)
        return result
    }

    private fun getContentBounds(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        Timber.d("bitmap size: %d x %d", width, height)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var left = width
        var top = height
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (Color.alpha(pixels[row + x]) != 0) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }

        return if (right < left || bottom < top) {
            null
        } else {
            Rect(left, top, right + 1, bottom + 1)
        }
    }

    companion object {
        // https://m3.material.io/styles/icons/designing-icons#089c3a26-5991-4241-8bbe-8a5ff2014247
        // 24dp (96px) icon, 20dp (80px) live area, 2dp (8px) padding per side
        private const val ICON_SIZE_PX = 96
        private const val PADDING_PER_SIDE_PX = 8
        private const val CONTENT_SIZE_PX = ICON_SIZE_PX - PADDING_PER_SIDE_PX * 2
    }
}
