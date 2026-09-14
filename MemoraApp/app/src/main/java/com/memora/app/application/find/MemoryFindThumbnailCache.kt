package com.memora.app.application.find

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-lifetime LRU of Find thumbnails. Access-order eviction. Not durable.
 */
@Singleton
class MemoryFindThumbnailCache @Inject constructor() : FindThumbnailCache {
    private val lock = Any()
    private val entries = object : LinkedHashMap<String, FindThumbnailResult>(
        FindThumbnailLimits.CACHE_ENTRIES,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, FindThumbnailResult>?,
        ): Boolean = size > FindThumbnailLimits.CACHE_ENTRIES
    }

    override fun get(key: String): FindThumbnailResult? {
        require(key.isNotBlank())
        synchronized(lock) {
            return entries[key]
        }
    }

    override fun put(key: String, value: FindThumbnailResult) {
        require(key.isNotBlank())
        if (!FindThumbnailPolicy.cacheable(value)) return
        synchronized(lock) {
            entries[key] = value
        }
    }

    override fun clear() {
        synchronized(lock) {
            entries.clear()
        }
    }

    internal fun sizeForTest(): Int = synchronized(lock) { entries.size }
}
