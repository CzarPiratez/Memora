package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnnxBgeSmallEnV15SpecTest {
    @Test
    fun product_pack_identity_and_integrity_pins_are_stable() {
        assertEquals("onnx-bge-small-en-v1.5", OnnxBgeSmallEnV15Spec.MODEL_IDENTITY.modelId)
        assertEquals("quantized-v1", OnnxBgeSmallEnV15Spec.MODEL_IDENTITY.version)
        assertEquals(384, OnnxBgeSmallEnV15Spec.EMBEDDING_DIMENSIONS)
        assertTrue(OnnxBgeSmallEnV15Spec.QUERY_PREFIX.startsWith("Represent this sentence"))
        assertEquals(64, OnnxBgeSmallEnV15Spec.EXPECTED_SHA256.length)
        assertTrue(OnnxBgeSmallEnV15Spec.DOWNLOAD_URL.contains("bge-small-en-v1.5"))
        assertTrue(OnnxBgeSmallEnV15Spec.MAX_DOWNLOAD_BYTES >= OnnxBgeSmallEnV15Spec.EXPECTED_BYTES)
    }
}
