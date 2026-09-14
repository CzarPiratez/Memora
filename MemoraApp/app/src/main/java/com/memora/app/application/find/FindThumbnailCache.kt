package com.memora.app.application.find

/**
 * Memory-only thumbnail store. Implementations must not write user pixels to disk.
 */
interface FindThumbnailCache {
    fun get(key: String): FindThumbnailResult?

    fun put(key: String, value: FindThumbnailResult)

    fun clear()
}
