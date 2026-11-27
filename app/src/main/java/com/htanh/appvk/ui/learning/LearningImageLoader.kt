package com.htanh.appvk.ui.learning

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Small HTTPS-only image loader for public learning-content media.
 * It intentionally avoids adding a third-party image dependency to this first slice.
 */
internal object LearningImageLoader {
    private const val MAX_DOWNLOAD_BYTES = 5 * 1024 * 1024
    private const val MAX_DECODE_DIMENSION = 1280

    private val memoryCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun load(imageView: ImageView, rawUrl: String?, scope: CoroutineScope) {
        imageView.setImageDrawable(null)
        val url = rawUrl?.trim().orEmpty()
        imageView.tag = url
        if (url.isBlank() || Uri.parse(url).scheme?.equals("https", ignoreCase = true) != true) {
            return
        }

        memoryCache.get(url)?.let {
            imageView.setImageBitmap(it)
            return
        }

        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { downloadAndDecode(url) }
            if (imageView.tag == url && bitmap != null) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun downloadAndDecode(url: String): Bitmap? {
        val connection = (URL(url).openConnection() as? HttpsURLConnection) ?: return null
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "image/*")
            connection.connect()

            if (connection.responseCode !in 200..299) return null
            if (connection.contentType?.startsWith("image/", ignoreCase = true) != true) return null
            if (connection.contentLengthLong > MAX_DOWNLOAD_BYTES) return null

            val bytes = connection.inputStream.use { input ->
                ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_DOWNLOAD_BYTES) return null
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also {
                memoryCache.put(url, it)
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_DECODE_DIMENSION ||
            height / sampleSize > MAX_DECODE_DIMENSION
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
