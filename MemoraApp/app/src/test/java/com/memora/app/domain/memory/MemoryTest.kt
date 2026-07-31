package com.memora.app.domain.memory

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MemoryTest {
    @Test
    fun `memory keeps a searchable signature tied to source evidence`() {
        val memory = memory()

        assertEquals("Lake picnic with Ana", memory.signature.summary.text.value)
        assertEquals(MemoryAnchorKind.PERSON, memory.signature.anchors.single().kind)
        assertEquals(setOf(MemoryEvidenceId("ocr-1")), memory.signature.anchors.single().evidenceIds)
    }

    @Test
    fun `memory rejects an anchor that cites missing evidence`() {
        assertThrows(IllegalArgumentException::class.java) {
            memory(
                anchorEvidenceIds = setOf(MemoryEvidenceId("not-stored")),
            )
        }
    }

    @Test
    fun `memory rejects duplicate evidence and backwards timestamps`() {
        val evidence = evidence()

        assertThrows(IllegalArgumentException::class.java) {
            memory(evidence = listOf(evidence, evidence))
        }
        assertThrows(IllegalArgumentException::class.java) {
            memory(
                createdAt = Instant.parse("2026-07-18T11:00:00Z"),
                updatedAt = Instant.parse("2026-07-18T10:00:00Z"),
            )
        }
    }

    private fun memory(
        evidence: List<MemoryEvidence> = listOf(evidence()),
        anchorEvidenceIds: Set<MemoryEvidenceId> = setOf(MemoryEvidenceId("ocr-1")),
        createdAt: Instant = Instant.parse("2026-07-18T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-07-18T10:05:00Z"),
    ): Memory = Memory(
        id = MemoryId("memory-1"),
        revisionId = MemoryRevisionId("revision-1"),
        assetIdentity = AssetIdentity(
            sourceId = SourceId("android-media-store"),
            sourceAssetKey = SourceAssetKey("image-42"),
        ),
        assetFingerprint = AssetFingerprint("media:42:1000:2048"),
        assemblySchemaVersion = MemoryAssemblySchemaVersion("asset-memory-v1"),
        extractionSchemaVersions = setOf("ocr-v1"),
        integrityState = MemoryIntegrityState.READY,
        signature = MemorySignature(
            summary = MemorySummary(
                text = MemoryText("Lake picnic with Ana"),
                evidenceIds = setOf(MemoryEvidenceId("ocr-1")),
            ),
            anchors = listOf(
                MemoryAnchor(
                    id = MemoryAnchorId("person-ana"),
                    kind = MemoryAnchorKind.PERSON,
                    text = MemoryText("Ana"),
                    evidenceIds = anchorEvidenceIds,
                )
            ),
        ),
        evidence = evidence,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun evidence(): MemoryEvidence = MemoryEvidence(
        id = MemoryEvidenceId("ocr-1"),
        kind = MemoryEvidenceKind.OCR_TEXT,
        locator = EvidenceLocator("image:whole"),
        excerpt = MemoryText("Ana's lake picnic"),
    )
}
