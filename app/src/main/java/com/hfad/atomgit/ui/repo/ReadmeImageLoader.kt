package com.hfad.atomgit.ui.repo

import android.graphics.BitmapFactory
import androidx.annotation.MainThread
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val ReadmeImageCacheBytes = 24 * 1024 * 1024

internal class ReadmeImageLoader(
    parentScope: CoroutineScope,
    private val fetch: suspend (String) -> ByteArray?,
) {
    private class Request(val result: Deferred<ImagePreviewTarget.Local?>, var consumers: Int = 0)

    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val requests = mutableMapOf<String, Request>()
    private val permits = Semaphore(4)
    private val cache = object : LruCache<String, ImagePreviewTarget.Local>(ReadmeImageCacheBytes) {
        override fun sizeOf(key: String, value: ImagePreviewTarget.Local): Int = value.memorySize.toInt()
    }

    @MainThread
    fun cached(path: String): ImagePreviewTarget.Local? = cache[path]

    @MainThread
    suspend fun load(path: String): ImagePreviewTarget.Local? {
        cached(path)?.let { return it }
        if (!job.isActive) return null
        val request = requests.getOrPut(path) {
            Request(scope.async(start = CoroutineStart.LAZY) {
                permits.withPermit {
                    val bytes = fetch(path) ?: return@withPermit null
                    val thumbnail = decodeReadmeImage(bytes, maxSide = 1280) ?: return@withPermit null
                    val image = ImagePreviewTarget.Local(path, bytes, thumbnail)
                    if (image.memorySize <= ReadmeImageCacheBytes) cache.put(path, image)
                    image
                }
            })
        }
        request.consumers++
        try {
            return request.result.await()
        } finally {
            if (--request.consumers == 0 && requests[path] === request) {
                requests.remove(path)
                request.result.cancel()
            }
        }
    }

    @MainThread
    fun close() {
        job.cancel()
        requests.clear()
        cache.evictAll()
    }
}

internal suspend fun decodeReadmeImage(bytes: ByteArray, maxSide: Int): ImageBitmap? =
    withContext(Dispatchers.Default) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null
        options.inJustDecodeBounds = false
        options.inSampleSize = 1
        val longestSide = maxOf(options.outWidth, options.outHeight)
        while (longestSide / options.inSampleSize > maxSide) options.inSampleSize *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }
