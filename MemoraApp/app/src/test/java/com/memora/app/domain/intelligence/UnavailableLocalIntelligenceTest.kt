package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityAvailabilityTest {
    @Test
    fun available_requires_model_identity() {
        val model = ModelVersionIdentity(modelId = "vision-a", version = "1.0.0")
        val availability = CapabilityAvailability.Available(model)
        assertEquals(model, availability.model)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unavailable_rejects_blank_reason() {
        CapabilityAvailability.Unavailable(" ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun model_identity_rejects_blank_version() {
        ModelVersionIdentity(modelId = "ocr-a", version = " ")
    }
}

class UnavailableLocalIntelligenceTest {
    @Test
    fun every_spec_capability_reports_unavailable_without_inventing_limits() {
        val engines = UnavailableLocalIntelligence.all()
        assertEquals(6, engines.size)
        assertEquals(
            setOf(
                CapabilityId.VISION,
                CapabilityId.OCR,
                CapabilityId.DOCUMENT,
                CapabilityId.EMBEDDING,
                CapabilityId.MEMORY_BUILDER,
                CapabilityId.RECALL_RANKER,
            ),
            engines.map { it.capabilityId }.toSet(),
        )

        engines.forEach { engine ->
            val availability = engine.availability()
            assertTrue(
                "Expected unavailable for ${engine.capabilityId}, was $availability",
                availability is CapabilityAvailability.Unavailable,
            )
            assertTrue(
                (availability as CapabilityAvailability.Unavailable).reason.isNotBlank(),
            )
            assertNull(engine.limits())
        }
    }

    @Test
    fun typed_stubs_match_capability_ids() {
        assertEquals(CapabilityId.VISION, UnavailableLocalIntelligence.vision().capabilityId)
        assertEquals(CapabilityId.OCR, UnavailableLocalIntelligence.ocr().capabilityId)
        assertEquals(CapabilityId.DOCUMENT, UnavailableLocalIntelligence.document().capabilityId)
        assertEquals(CapabilityId.EMBEDDING, UnavailableLocalIntelligence.embedding().capabilityId)
        assertEquals(
            CapabilityId.MEMORY_BUILDER,
            UnavailableLocalIntelligence.memoryBuilder().capabilityId,
        )
        assertEquals(
            CapabilityId.RECALL_RANKER,
            UnavailableLocalIntelligence.recallRanker().capabilityId,
        )
    }

    @Test
    fun custom_unavailable_reason_is_preserved() {
        val reason = "Vision pack is incompatible with this device."
        val availability = UnavailableLocalIntelligence.vision(reason).availability()
        assertEquals(
            CapabilityAvailability.Unavailable(reason),
            availability,
        )
    }
}
