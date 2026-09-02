package com.memora.app.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioSegmentEvidenceLocatorTest {
    @Test
    fun formatAndParse_roundTrip() {
        val locator = AudioSegmentEvidenceLocator.formatLocator(startMs = 872_000, endMs = 879_500)
        assertEquals("audio:segment:872000:879500", locator)
        val range = AudioSegmentEvidenceLocator.parseSegment(locator)
        assertEquals(872_000L, range?.startMs)
        assertEquals(879_500L, range?.endMs)
    }

    @Test
    fun parse_rejectsInvalid() {
        assertNull(AudioSegmentEvidenceLocator.parseSegment("pdf:page:1"))
        assertNull(AudioSegmentEvidenceLocator.parseSegment("audio:segment:1"))
        assertNull(AudioSegmentEvidenceLocator.parseSegment("audio:segment:5:3"))
        assertNull(AudioSegmentEvidenceLocator.parseSegment(""))
    }

    @Test
    fun evidenceIdForSegment_resolvesMatchingEvidence() {
        val evidence = listOf(
            MemoryEvidence(
                id = MemoryEvidenceId("e1"),
                kind = MemoryEvidenceKind.DOCUMENT_TEXT,
                evidenceClass = MemoryEvidenceClass.DIRECT,
                locator = EvidenceLocator("pdf:page:1"),
                excerpt = MemoryText("page"),
            ),
            MemoryEvidence(
                id = MemoryEvidenceId("e2"),
                kind = MemoryEvidenceKind.DOCUMENT_TEXT,
                evidenceClass = MemoryEvidenceClass.DIRECT,
                locator = EvidenceLocator(AudioSegmentEvidenceLocator.formatLocator(100, 200)),
                excerpt = MemoryText("segment"),
            ),
        )
        assertEquals(MemoryEvidenceId("e2"), AudioSegmentEvidenceLocator.evidenceIdForSegment(evidence, 100, 200))
        assertNull(AudioSegmentEvidenceLocator.evidenceIdForSegment(evidence, 100, 201))
    }
}
