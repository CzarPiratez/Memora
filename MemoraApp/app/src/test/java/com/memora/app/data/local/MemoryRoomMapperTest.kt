package com.memora.app.data.local

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAnchor
import com.memora.app.domain.memory.MemoryAnchorId
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEvidence
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.MemorySignature
import com.memora.app.domain.memory.MemorySummary
import com.memora.app.domain.memory.MemoryText
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryRoomMapperTest {
    @Test
    fun `normalized Room rows round trip every citation and revision field`() {
        val evidence = listOf(
            MemoryEvidence(
                MemoryEvidenceId("e1"),
                MemoryEvidenceKind.OCR_TEXT,
                EvidenceLocator("image:whole"),
                MemoryText("Boarding gate B7"),
            ),
            MemoryEvidence(
                MemoryEvidenceId("e2"),
                MemoryEvidenceKind.SOURCE_METADATA,
                EvidenceLocator("exif:fields"),
                MemoryText("Date taken: 2026-07-31"),
            ),
        )
        val memory = Memory(
            id = MemoryId("memory"),
            revisionId = MemoryRevisionId("revision"),
            assetIdentity = AssetIdentity(SourceId("source"), SourceAssetKey("asset")),
            assetFingerprint = AssetFingerprint("fingerprint"),
            assemblySchemaVersion = MemoryAssemblySchemaVersion("facts-v1"),
            extractionSchemaVersions = setOf("ocr-v1", "exif-v1"),
            integrityState = MemoryIntegrityState.READY,
            signature = MemorySignature(
                summary = MemorySummary(MemoryText("Boarding gate B7"), setOf(evidence[0].id)),
                anchors = listOf(
                    MemoryAnchor(
                        MemoryAnchorId("text-1"),
                        MemoryAnchorKind.TEXT,
                        MemoryText("Boarding gate B7"),
                        setOf(evidence[0].id, evidence[1].id),
                    ),
                ),
            ),
            evidence = evidence,
            createdAt = Instant.parse("2026-07-31T10:00:00Z"),
            updatedAt = Instant.parse("2026-07-31T10:00:00Z"),
        )

        val restored = MemoryRoomMapper.toDomain(MemoryRoomMapper.toRows(memory))

        assertEquals(memory, restored)
    }
}
