package com.memora.app.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AssemblyFactsDigestTest {
    @Test
    fun empty_facts_are_none() {
        assertEquals(AssemblyFactsDigest.NONE, AssemblyFactsDigest.of(emptyList()))
    }

    @Test
    fun order_does_not_change_the_digest() {
        val first = AssetMemoryFact(
            MemoryEvidenceKind.DOCUMENT_TEXT,
            "pdf:page:1",
            "hello",
            "pdf-extraction-v1",
        )
        val second = AssetMemoryFact(
            MemoryEvidenceKind.SOURCE_METADATA,
            "pdf:title",
            "Title: timetable",
            "pdf-extraction-v1",
        )
        assertEquals(
            AssemblyFactsDigest.of(listOf(first, second)),
            AssemblyFactsDigest.of(listOf(second, first)),
        )
    }

    @Test
    fun a_new_excerpt_changes_the_digest() {
        val before = AssetMemoryFact(
            MemoryEvidenceKind.OCR_TEXT,
            "image:whole",
            "scan",
            "screenshot-ocr-v1",
        )
        val after = AssetMemoryFact(
            MemoryEvidenceKind.OCR_TEXT,
            "image:whole",
            "scan silky",
            "screenshot-ocr-v1",
        )
        assertNotEquals(AssemblyFactsDigest.of(listOf(before)), AssemblyFactsDigest.of(listOf(after)))
    }
}
