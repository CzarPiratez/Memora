package com.memora.app.application.memory

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MIG-06 step 1: additive SearchMemoryEvidence contract (no UI cutover). */
class SearchMemoryEvidenceTest {
    private val schema = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA

    @Test
    fun blank_query_is_honest() = runBlocking {
        val search = SearchMemoryEvidence(FakeExcerptSearch(rows = listOf(metadataRow())))
        assertEquals(MemoryEvidenceSearchOutcome.BlankQuery, search("   "))
        assertEquals(MemoryEvidenceSearchOutcome.BlankQuery, search(""))
    }

    @Test
    fun nothing_saved_when_corpus_empty() = runBlocking {
        val search = SearchMemoryEvidence(FakeExcerptSearch(rows = emptyList()))
        val outcome = search("invoice")
        assertEquals(
            MemoryEvidenceSearchOutcome.NothingSavedToSearch(query = "invoice"),
            outcome,
        )
    }

    @Test
    fun matches_all_current_evidence_kinds_with_provenance() = runBlocking {
        val rows = listOf(
            metadataRow(excerpt = "EXIF date Taken: 2024-06-01 invoice trip"),
            ocrRow(excerpt = "Screenshot OCR shows invoice total $42"),
            documentRow(excerpt = "PDF page text about the invoice clause"),
            noteRow(excerpt = "OneNote reminder: pay invoice Friday"),
        )
        val search = SearchMemoryEvidence(FakeExcerptSearch(rows = rows))
        val outcome = search("invoice") as MemoryEvidenceSearchOutcome.Matches

        assertEquals("invoice", outcome.query)
        assertFalse(outcome.limitReached)
        assertEquals(4, outcome.hits.size)

        val byKind = outcome.hits.associateBy { it.kind }
        assertEquals(
            setOf(
                MemoryEvidenceKind.SOURCE_METADATA,
                MemoryEvidenceKind.OCR_TEXT,
                MemoryEvidenceKind.DOCUMENT_TEXT,
                MemoryEvidenceKind.NOTE_TEXT,
            ),
            byKind.keys,
        )

        val meta = byKind.getValue(MemoryEvidenceKind.SOURCE_METADATA)
        assertEquals(MemoryId("mem-meta"), meta.memoryId)
        assertEquals(MemoryRevisionId("rev-meta"), meta.revisionId)
        assertEquals(MemoryEvidenceId("e-meta"), meta.evidenceId)
        assertEquals(EvidenceLocator("exif:date_taken"), meta.locator)
        assertEquals(SourceId("android-media-store"), meta.sourceId)
        assertEquals(SourceAssetKey("photo-1"), meta.sourceAssetKey)
        assertEquals(AssetType.PHOTO, meta.assetType)
        assertEquals("Lake photo", meta.label)
        assertTrue(meta.excerpt.contains("invoice", ignoreCase = true))
        assertEquals(MemoryEvidenceRetrievalPath.KEYWORD, meta.retrievalPath)
        assertNull(meta.openPageNumber)

        val doc = byKind.getValue(MemoryEvidenceKind.DOCUMENT_TEXT)
        assertEquals(AssetType.PDF, doc.assetType)
        assertEquals(3, doc.openPageNumber)
        assertEquals(PdfPageEvidenceLocator.formatLocator(3), doc.locator.value)
        assertTrue(doc.excerpt.contains("invoice", ignoreCase = true))

        val ocr = byKind.getValue(MemoryEvidenceKind.OCR_TEXT)
        assertEquals(AssetType.SCREENSHOT, ocr.assetType)
        assertNull(ocr.openPageNumber)

        val note = byKind.getValue(MemoryEvidenceKind.NOTE_TEXT)
        assertEquals(AssetType.NOTE, note.assetType)
        assertEquals(SourceId("microsoft-onenote"), note.sourceId)
    }

    @Test
    fun non_matching_query_returns_empty_matches_when_corpus_exists() = runBlocking {
        val search = SearchMemoryEvidence(
            FakeExcerptSearch(rows = listOf(documentRow(excerpt = "unrelated PDF text"))),
        )
        val outcome = search("invoice") as MemoryEvidenceSearchOutcome.Matches
        assertTrue(outcome.hits.isEmpty())
        assertFalse(outcome.limitReached)
    }

    @Test
    fun like_special_characters_are_treated_literally() = runBlocking {
        val row = documentRow(
            excerpt = "Cost is 100%_refundable for premium",
            evidenceId = "e-special",
        )
        val search = SearchMemoryEvidence(FakeExcerptSearch(rows = listOf(row)))
        val outcome = search("100%_refund") as MemoryEvidenceSearchOutcome.Matches
        assertEquals(1, outcome.hits.size)
        assertTrue(outcome.hits.single().excerpt.contains("100%_refund", ignoreCase = true))
    }

    @Test
    fun normalize_collapses_whitespace_and_caps_length() {
        assertEquals("a b", MemoryEvidenceLiteralSearchSupport.normalizeQuery("  a   b  "))
        assertNull(MemoryEvidenceLiteralSearchSupport.normalizeQuery(" \t "))
        val long = "x".repeat(200)
        assertEquals(
            120,
            MemoryEvidenceLiteralSearchSupport.normalizeQuery(long)!!.length,
        )
    }

    private fun metadataRow(
        excerpt: String = "metadata excerpt",
    ): MemoryEvidenceExcerptMatch = MemoryEvidenceExcerptMatch(
        memoryId = MemoryId("mem-meta"),
        revisionId = MemoryRevisionId("rev-meta"),
        evidenceId = MemoryEvidenceId("e-meta"),
        kind = MemoryEvidenceKind.SOURCE_METADATA,
        locator = EvidenceLocator("exif:date_taken"),
        excerpt = excerpt,
        sourceId = SourceId("android-media-store"),
        sourceAssetKey = SourceAssetKey("photo-1"),
        assetType = AssetType.PHOTO,
        displayLabel = "Lake photo",
    )

    private fun ocrRow(
        excerpt: String = "ocr excerpt",
    ): MemoryEvidenceExcerptMatch = MemoryEvidenceExcerptMatch(
        memoryId = MemoryId("mem-ocr"),
        revisionId = MemoryRevisionId("rev-ocr"),
        evidenceId = MemoryEvidenceId("e-ocr"),
        kind = MemoryEvidenceKind.OCR_TEXT,
        locator = EvidenceLocator("image:whole"),
        excerpt = excerpt,
        sourceId = SourceId("android-media-store"),
        sourceAssetKey = SourceAssetKey("shot-1"),
        assetType = AssetType.SCREENSHOT,
        displayLabel = "Receipt screenshot",
    )

    private fun documentRow(
        excerpt: String = "document excerpt",
        evidenceId: String = "e-doc",
    ): MemoryEvidenceExcerptMatch = MemoryEvidenceExcerptMatch(
        memoryId = MemoryId("mem-doc"),
        revisionId = MemoryRevisionId("rev-doc"),
        evidenceId = MemoryEvidenceId(evidenceId),
        kind = MemoryEvidenceKind.DOCUMENT_TEXT,
        locator = EvidenceLocator(PdfPageEvidenceLocator.formatLocator(3)),
        excerpt = excerpt,
        sourceId = SourceId("saf-document-tree"),
        sourceAssetKey = SourceAssetKey("pdf-1"),
        assetType = AssetType.PDF,
        displayLabel = "Contract.pdf",
    )

    private fun noteRow(
        excerpt: String = "note excerpt",
    ): MemoryEvidenceExcerptMatch = MemoryEvidenceExcerptMatch(
        memoryId = MemoryId("mem-note"),
        revisionId = MemoryRevisionId("rev-note"),
        evidenceId = MemoryEvidenceId("e-note"),
        kind = MemoryEvidenceKind.NOTE_TEXT,
        locator = EvidenceLocator("onenote:page"),
        excerpt = excerpt,
        sourceId = SourceId("microsoft-onenote"),
        sourceAssetKey = SourceAssetKey("note-1"),
        assetType = AssetType.NOTE,
        displayLabel = "Shopping list",
    )

    /**
     * In-memory port that mirrors SQLite LIKE case-insensitive ASCII substring
     * matching (after the use case escapes the needle).
     */
    private class FakeExcerptSearch(
        private val rows: List<MemoryEvidenceExcerptMatch>,
    ) : MemoryEvidenceExcerptSearch {
        override suspend fun countCurrentReadyEvidence(assemblySchemaVersion: String): Int =
            rows.size

        override suspend fun searchByExcerpt(
            escapedNeedle: String,
            assemblySchemaVersion: String,
            limit: Int,
        ): List<MemoryEvidenceExcerptMatch> {
            val literal = unescapeLike(escapedNeedle)
            return rows
                .filter { it.excerpt.contains(literal, ignoreCase = true) }
                .take(limit)
        }

        private fun unescapeLike(escaped: String): String = buildString(escaped.length) {
            var i = 0
            while (i < escaped.length) {
                val ch = escaped[i]
                if (ch == '\\' && i + 1 < escaped.length) {
                    append(escaped[i + 1])
                    i += 2
                } else {
                    append(ch)
                    i += 1
                }
            }
        }
    }
}
