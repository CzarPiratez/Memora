package com.memora.app.application.memory

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.memory.AssetMemoryFact
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAnchor
import com.memora.app.domain.memory.MemoryAnchorId
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEvidence
import com.memora.app.domain.memory.MemoryEvidenceClass
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.MemorySignature
import com.memora.app.domain.memory.MemorySummary
import com.memora.app.domain.memory.MemoryText
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import javax.inject.Inject

/**
 * Builds a truthful pre-AI Asset Memory from already-persisted deterministic facts.
 *
 * This does not infer people, places, topics, meaning, or confidence. Summary and
 * TEXT anchor are bounded excerpts of the same evidence persisted with the revision.
 */
class AssembleAssetMemoryFromExtractionFacts internal constructor(
    private val assetRepository: AssetRepository,
    private val factSource: AssetMemoryFactSource,
    private val memoryRepository: MemoryRepository,
    private val clock: Clock,
) {
    @Inject
    constructor(
        assetRepository: AssetRepository,
        factSource: AssetMemoryFactSource,
        memoryRepository: MemoryRepository,
    ) : this(assetRepository, factSource, memoryRepository, Clock.systemUTC())

    suspend operator fun invoke(identity: AssetIdentity): AssetMemoryAssemblyResult {
        val record = assetRepository.find(identity) ?: return AssetMemoryAssemblyResult.AssetMissing
        val asset = record.asset
        val existing = memoryRepository.find(asset.identity, asset.fingerprint, ASSEMBLY_SCHEMA)
        if (existing != null) return AssetMemoryAssemblyResult.AlreadyPresent(existing)

        val facts = factSource.loadCurrentFacts(asset)
            .mapNotNull(::sanitize)
            .distinctBy { Triple(it.kind, it.locator, it.excerpt) }
            .take(MAX_EVIDENCE_ITEMS)
        if (facts.isEmpty()) return AssetMemoryAssemblyResult.NoUsableEvidence

        val evidence = facts.mapIndexed { index, fact ->
            MemoryEvidence(
                id = MemoryEvidenceId("e${index + 1}"),
                kind = fact.kind,
                evidenceClass = MemoryEvidenceClass.DIRECT,
                locator = EvidenceLocator(fact.locator),
                excerpt = MemoryText(fact.excerpt),
            )
        }
        val primary = selectPrimaryEvidence(evidence)
        val now = clock.instant()
        val memoryId = MemoryId(hash(asset.identity.sourceId.value, asset.identity.sourceAssetKey.value))
        val memory = Memory(
            id = memoryId,
            revisionId = MemoryRevisionId(
                hash(memoryId.value, asset.fingerprint.value, ASSEMBLY_SCHEMA.value),
            ),
            assetIdentity = asset.identity,
            assetFingerprint = asset.fingerprint,
            assemblySchemaVersion = ASSEMBLY_SCHEMA,
            extractionSchemaVersions = facts.mapTo(linkedSetOf()) {
                it.extractionSchemaVersion
            },
            integrityState = MemoryIntegrityState.READY,
            signature = MemorySignature(
                summary = MemorySummary(
                    text = MemoryText(primary.excerpt.value.take(MAX_SUMMARY_CHARS)),
                    evidenceIds = setOf(primary.id),
                ),
                anchors = listOf(
                    MemoryAnchor(
                        id = MemoryAnchorId("text-1"),
                        kind = MemoryAnchorKind.TEXT,
                        text = MemoryText(primary.excerpt.value.take(MAX_ANCHOR_CHARS)),
                        evidenceIds = setOf(primary.id),
                    ),
                ),
            ),
            evidence = evidence,
            createdAt = now,
            updatedAt = now,
        )

        return when (memoryRepository.insert(memory)) {
            MemoryInsertResult.Inserted -> AssetMemoryAssemblyResult.Persisted(memory)
            MemoryInsertResult.AlreadyExists -> AssetMemoryAssemblyResult.AlreadyPresent(
                memoryRepository.find(asset.identity, asset.fingerprint, ASSEMBLY_SCHEMA) ?: memory,
            )
            MemoryInsertResult.RevisionConflict -> AssetMemoryAssemblyResult.RevisionConflict
            MemoryInsertResult.FailedSafely -> AssetMemoryAssemblyResult.FailedSafely
        }
    }

    private fun sanitize(fact: AssetMemoryFact): AssetMemoryFact? {
        val excerpt = fact.excerpt.replace(WHITESPACE, " ").trim().take(MAX_EVIDENCE_CHARS)
        val locator = fact.locator.trim().take(MAX_LOCATOR_CHARS)
        if (excerpt.isBlank() || locator.isBlank()) return null
        if (fact.kind == MemoryEvidenceKind.SOURCE_METADATA && isWeakExifNoise(excerpt)) {
            return null
        }
        return fact.copy(excerpt = excerpt, locator = locator)
    }

    /**
     * Prefer OCR / document / note text for the Memory summary so meaning search
     * embeds recallable content instead of EXIF dimension noise.
     */
    private fun selectPrimaryEvidence(evidence: List<MemoryEvidence>): MemoryEvidence {
        require(evidence.isNotEmpty())
        return evidence.firstOrNull { it.kind.isPrimaryTextKind() } ?: evidence.first()
    }

    private fun MemoryEvidenceKind.isPrimaryTextKind(): Boolean =
        this == MemoryEvidenceKind.OCR_TEXT ||
            this == MemoryEvidenceKind.DOCUMENT_TEXT ||
            this == MemoryEvidenceKind.NOTE_TEXT

    /** Dimensions / orientation alone are not useful meaning cues. */
    private fun isWeakExifNoise(excerpt: String): Boolean {
        val parts = excerpt.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return true
        return parts.all { part ->
            part.startsWith("Dimensions:", ignoreCase = true) ||
                part.startsWith("Orientation:", ignoreCase = true)
        }
    }

    private fun hash(vararg parts: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        parts.forEachIndexed { index, part ->
            if (index > 0) digest.update(UNIT_SEPARATOR)
            digest.update(part.toByteArray(StandardCharsets.UTF_8))
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        /** v2: OCR/text-first summaries; no dimension-only EXIF memories. */
        val ASSEMBLY_SCHEMA = MemoryAssemblySchemaVersion("asset-memory-facts-v2")
        const val MAX_EVIDENCE_ITEMS = 8
        const val MAX_EVIDENCE_CHARS = 500
        const val MAX_SUMMARY_CHARS = 240
        const val MAX_ANCHOR_CHARS = 160
        const val MAX_LOCATOR_CHARS = 160
        private val WHITESPACE = Regex("\\s+")
        private val UNIT_SEPARATOR = byteArrayOf(0x1f)
    }
}

sealed interface AssetMemoryAssemblyResult {
    data class Persisted(val memory: Memory) : AssetMemoryAssemblyResult
    data class AlreadyPresent(val memory: Memory) : AssetMemoryAssemblyResult
    data object AssetMissing : AssetMemoryAssemblyResult
    data object NoUsableEvidence : AssetMemoryAssemblyResult
    data object RevisionConflict : AssetMemoryAssemblyResult
    data object FailedSafely : AssetMemoryAssemblyResult
}
