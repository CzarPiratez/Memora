package com.memora.app.data.security

/**
 * Coarse, content-free elapsed-time buckets for conversion observability.
 * Never encode row contents, paths, or secrets — only timing ranges.
 */
object ConversionElapsedBuckets {
    fun forMillis(elapsedMillis: Long): String = when {
        elapsedMillis < 1_000L -> "lt_1s"
        elapsedMillis < 5_000L -> "1s_to_5s"
        elapsedMillis < 15_000L -> "5s_to_15s"
        elapsedMillis < 60_000L -> "15s_to_60s"
        elapsedMillis < 180_000L -> "60s_to_180s"
        else -> "gte_180s"
    }
}
