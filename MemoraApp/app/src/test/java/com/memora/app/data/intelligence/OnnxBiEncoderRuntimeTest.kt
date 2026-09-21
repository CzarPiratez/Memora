package com.memora.app.data.intelligence

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class OnnxBiEncoderRuntimeTest {
    @Test
    fun cls_pool_takes_leading_token_from_sequence() {
        val raw = floatArrayOf(
            1f, 2f, 3f,
            9f, 9f, 9f,
            8f, 8f, 8f,
        )
        val pooled = OnnxBiEncoderRuntime.clsPool(raw, sequenceLength = 3, dimensions = 3)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), pooled, 0f)
    }

    @Test
    fun cls_pool_accepts_already_pooled_vector() {
        val raw = floatArrayOf(0.5f, -0.5f)
        val pooled = OnnxBiEncoderRuntime.clsPool(raw, sequenceLength = 4, dimensions = 2)
        assertArrayEquals(raw, pooled, 0f)
    }

    @Test
    fun l2_normalize_unit_length() {
        val normalized = OnnxBiEncoderRuntime.l2Normalize(floatArrayOf(3f, 4f))
        assertEquals(0.6f, normalized[0], 1e-5f)
        assertEquals(0.8f, normalized[1], 1e-5f)
        val length = sqrt(
            (normalized[0] * normalized[0] + normalized[1] * normalized[1]).toDouble(),
        )
        assertEquals(1.0, length, 1e-5)
    }
}
