package com.memora.app.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionElapsedBucketsTest {
    @Test
    fun maps_elapsed_millis_to_coarse_content_free_buckets() {
        assertEquals("lt_1s", ConversionElapsedBuckets.forMillis(0))
        assertEquals("lt_1s", ConversionElapsedBuckets.forMillis(999))
        assertEquals("1s_to_5s", ConversionElapsedBuckets.forMillis(1_000))
        assertEquals("5s_to_15s", ConversionElapsedBuckets.forMillis(5_000))
        assertEquals("15s_to_60s", ConversionElapsedBuckets.forMillis(15_000))
        assertEquals("60s_to_180s", ConversionElapsedBuckets.forMillis(60_000))
        assertEquals("gte_180s", ConversionElapsedBuckets.forMillis(180_000))
    }
}
