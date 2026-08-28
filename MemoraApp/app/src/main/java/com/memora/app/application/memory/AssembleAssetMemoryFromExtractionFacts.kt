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
 * This does not invent people, places, objects, activities, purposes, meaning, or
 * confidence. Every usable deterministic fact becomes one [MemoryEvidence] row
 * (DIRECT). Summary and anchors are display-bounded excerpts of evidence already
 * persisted with the revision; those display bounds are not evidence-completeness
 * limits. TIME and TOPIC anchors are added only when EXIF date-taken or PDF/note
 * title facts survive sanitize (MIG-03).
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
        val anchors = buildAnchors(evidence, primary)
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
                anchors = anchors,
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
        val excerpt = fact.excerpt.replace(WHITESPACE, " ").trim()
            .take(MAX_EVIDENCE_CHARS_PER_ITEM)
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

    /**
     * TEXT always; TIME when a surviving EXIF date-taken fact exists; TOPIC when a
     * PDF or note title fact exists. Never fabricates PERSON/PLACE/OBJECT/ACTIVITY/
     * PURPOSE. Every anchor cites evidence already on the revision.
     */
    private fun buildAnchors(
        evidence: List<MemoryEvidence>,
        primary: MemoryEvidence,
    ): List<MemoryAnchor> = buildList {
        add(
            MemoryAnchor(
                id = MemoryAnchorId("text-1"),
                kind = MemoryAnchorKind.TEXT,
                text = MemoryText(primary.excerpt.value.take(MAX_ANCHOR_CHARS)),
                evidenceIds = setOf(primary.id),
            ),
        )
        findTimeEvidence(evidence)?.let { timeEvidence ->
            val timeText = dateTakenField(timeEvidence.excerpt.value)
                ?: timeEvidence.excerpt.value
            add(
                MemoryAnchor(
                    id = MemoryAnchorId("time-1"),
                    kind = MemoryAnchorKind.TIME,
                    text = MemoryText(timeText.take(MAX_ANCHOR_CHARS)),
                    evidenceIds = setOf(timeEvidence.id),
                ),
            )
        }
        findTopicEvidence(evidence)?.let { topicEvidence ->
            add(
                MemoryAnchor(
                    id = MemoryAnchorId("topic-1"),
                    kind = MemoryAnchorKind.TOPIC,
                    text = MemoryText(topicEvidence.excerpt.value.take(MAX_ANCHOR_CHARS)),
                    evidenceIds = setOf(topicEvidence.id),
                ),
            )
        }
    }

    private fun findTimeEvidence(evidence: List<MemoryEvidence>): MemoryEvidence? =
        evidence.firstOrNull { item ->
            item.kind == MemoryEvidenceKind.SOURCE_METADATA &&
                item.locator.value == EXIF_FIELDS_LOCATOR &&
                dateTakenField(item.excerpt.value) != null
        }

    private fun findTopicEvidence(evidence: List<MemoryEvidence>): MemoryEvidence? =
        evidence.firstOrNull { item ->
            item.kind == MemoryEvidenceKind.SOURCE_METADATA &&
                item.locator.value in TOPIC_TITLE_LOCATORS
        }

    /** Returns the Date-taken field from an EXIF excerpt, or null if absent. */
    private fun dateTakenField(excerpt: String): String? =
        excerpt.split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith(DATE_TAKEN_PREFIX, ignoreCase = true) }
            ?.takeIf { it.length > DATE_TAKEN_PREFIX.length }

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
        /**
         * v4 (MIG-03): TIME/TOPIC anchors from EXIF date-taken and PDF/note title
         * facts, in addition to uncapped DIRECT evidence (v3). Prior v3 revisions
         * remain readable; next legitimate assembly for the current schema produces
         * typed anchors without a forced mass reindex.
         */
        val ASSEMBLY_SCHEMA = MemoryAssemblySchemaVersion("asset-memory-facts-v4")

        /**
         * Pathological per-item guard, not a completeness policy. Caps one fact so a
         * single oversized OCR/PDF/note blob cannot dominate a Memory revision.
         * Aligned with the provisional PDF page write budget
         * (`PdfExtractionWriteBudgets.MAX_CHARS_PER_PAGE` = 8192) so accepted
         * extraction pages can become evidence without an extra artificial cut.
         */
        const val MAX_EVIDENCE_CHARS_PER_ITEM = 8_192

        /** UI-facing summary display bound only — not an evidence completeness limit. */
        const val MAX_SUMMARY_CHARS = 240
        const val MAX_ANCHOR_CHARS = 160
        const val MAX_LOCATOR_CHARS = 160
        private const val EXIF_FIELDS_LOCATOR = "exif:fields"
        private const val DATE_TAKEN_PREFIX = "Date taken:"
        private val TOPIC_TITLE_LOCATORS = setOf("pdf:title", "note:title")
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
