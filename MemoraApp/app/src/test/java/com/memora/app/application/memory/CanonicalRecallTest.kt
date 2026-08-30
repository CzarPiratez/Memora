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
import org.junit.Assert.assertTrue
import org.junit.Test

/** Thin CanonicalRecall façade delegates KEYWORD path to SearchMemoryEvidence. */
class CanonicalRecallTest {

    @Test
    fun invoke_delegatesKeywordSearchWithAssetTypeFilter() = runBlocking {
        val rows = listOf(
            documentRow(excerpt = "PDF page text about the budget clause"),
            noteRow(excerpt = "note budget"),
        )
        val recall = CanonicalRecall(SearchMemoryEvidence(FakeExcerptSearch(rows)))

        val outcome = recall(rawQuery = "budget", assetType = AssetType.PDF)
            as MemoryEvidenceSearchOutcome.Matches

        assertEquals(1, outcome.hits.size)
        assertEquals(AssetType.PDF, outcome.hits.single().assetType)
        assertEquals(MemoryEvidenceRetrievalPath.KEYWORD, outcome.hits.single().retrievalPath)
        assertTrue(outcome.hits.single().excerpt.contains("budget", ignoreCase = true))
    }

    @Test
    fun invoke_blankQuery_returnsBlank() = runBlocking {
        val recall = CanonicalRecall(
            SearchMemoryEvidence(FakeExcerptSearch(rows = listOf(documentRow()))),
        )
        assertEquals(MemoryEvidenceSearchOutcome.BlankQuery, recall(rawQuery = "   "))
    }

    private fun documentRow(
        excerpt: String = "document excerpt",
    ): MemoryEvidenceExcerptMatch = MemoryEvidenceExcerptMatch(
        memoryId = MemoryId("mem-doc"),
        revisionId = MemoryRevisionId("rev-doc"),
        evidenceId = MemoryEvidenceId("e-doc"),
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

    private class FakeExcerptSearch(
        private val rows: List<MemoryEvidenceExcerptMatch>,
    ) : MemoryEvidenceExcerptSearch {
        override suspend fun countCurrentReadyEvidence(
            assemblySchemaVersion: String,
            assetType: AssetType?,
        ): Int = rows.count { assetType == null || it.assetType == assetType }

        override suspend fun countCurrentReadyEvidenceCorpus(
            assemblySchemaVersion: String,
            assetType: AssetType?,
        ): MemoryEvidenceCorpusCounts {
            val filtered = rows.filter { assetType == null || it.assetType == assetType }
            val documents = filtered.map { it.sourceId.value to it.sourceAssetKey.value }.toSet()
            return MemoryEvidenceCorpusCounts(
                evidenceCount = filtered.size,
                documentCount = documents.size,
            )
        }

        override suspend fun searchByExcerpt(
            escapedNeedle: String,
            assemblySchemaVersion: String,
            limit: Int,
            assetType: AssetType?,
        ): List<MemoryEvidenceExcerptMatch> {
            val literal = unescapeLike(escapedNeedle)
            return rows
                .filter { assetType == null || it.assetType == assetType }
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
