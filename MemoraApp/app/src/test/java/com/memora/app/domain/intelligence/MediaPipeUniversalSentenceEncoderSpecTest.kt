package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPipeUniversalSentenceEncoderSpecTest {
    @Test
    fun product_spec_points_at_use_float32_artifact() {
        assertTrue(
            MediaPipeUniversalSentenceEncoderSpec.DOWNLOAD_URL.contains(
                "universal_sentence_encoder/float32/1",
            ),
        )
        assertEquals(
            "universal_sentence_encoder_float32_1.tflite",
            MediaPipeUniversalSentenceEncoderSpec.FILE_NAME,
        )
        assertEquals(
            "mediapipe-universal-sentence-encoder",
            MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY.modelId,
        )
        assertTrue(MediaPipeUniversalSentenceEncoderSpec.MAX_DOWNLOAD_BYTES > 8L * 1024L * 1024L)
        assertTrue(MediaPipeUniversalSentenceEncoderSpec.DISCLOSED_SIZE_MB_CEILING >= 30)
    }

    @Test
    fun legacy_average_word_spec_remains_for_upgrade_cleanup() {
        assertTrue(
            MediaPipeAverageWordEmbedderSpec.FILE_NAME.contains("average_word"),
        )
        assertTrue(
            MediaPipeAverageWordEmbedderSpec.MODEL_IDENTITY.modelId !=
                MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY.modelId,
        )
    }
}
