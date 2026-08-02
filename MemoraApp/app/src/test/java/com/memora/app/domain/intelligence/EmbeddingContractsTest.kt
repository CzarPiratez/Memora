package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingContractsTest {
    @Test
    fun cosine_similarity_is_one_for_identical_vectors() {
        val vector = EmbeddingVector(floatArrayOf(1f, 0f, 0f))
        assertEquals(1f, EmbeddingSimilarity.cosine(vector, vector), 0.0001f)
    }

    @Test
    fun unavailable_engine_refuses_to_invent_vectors() {
        val result = UnavailableEmbeddingEngine().embedText("cafe receipt")
        assertTrue(result is EmbeddingEncodeResult.Unavailable)
    }

    @Test(expected = IllegalArgumentException::class)
    fun embedding_vector_rejects_empty() {
        EmbeddingVector(floatArrayOf())
    }
}
