package com.memora.app.domain.memory

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import java.time.Instant

/** A stable identifier for a fact or excerpt that supports a memory. */
@JvmInline
value class MemoryEvidenceId(val value: String) {
    init {
        require(value.isNotBlank()) { "A memory evidence ID cannot be blank." }
    }
}

/** A stable identifier for one recall cue inside a memory signature. */
@JvmInline
value class MemoryAnchorId(val value: String) {
    init {
        require(value.isNotBlank()) { "A memory anchor ID cannot be blank." }
    }
}

/** Text that Memora may later search or show as an explanation. */
@JvmInline
value class MemoryText(val value: String) {
    init {
        require(value.isNotBlank()) { "Memory text cannot be blank." }
    }
}

/** The type of source-derived evidence that can support a memory. */
enum class MemoryEvidenceKind {
    SOURCE_METADATA,
    OCR_TEXT,
    DOCUMENT_TEXT,
    NOTE_TEXT,
    VISUAL_OBSERVATION,
}

/**
 * A source-specific pointer used to explain where an evidence item came from, such
 * as a metadata field, image region, PDF page, or note section. The source adapter
 * owns how this pointer is interpreted; it is never a direct file handle.
 */
@JvmInline
value class EvidenceLocator(val value: String) {
    init {
        require(value.isNotBlank()) { "An evidence locator cannot be blank." }
    }
}

/**
 * A deterministic or validated observation derived from the original Asset.
 *
 * [excerpt] is intentionally a small explainable fact, not an unbounded copy of the
 * original source. Later extractors decide which facts are safe and useful to retain.
 */
data class MemoryEvidence(
    val id: MemoryEvidenceId,
    val kind: MemoryEvidenceKind,
    val locator: EvidenceLocator,
    val excerpt: MemoryText,
)

/** The recall dimensions described by the product: what, who, where, when, and why. */
enum class MemoryAnchorKind {
    PERSON,
    PLACE,
    OBJECT,
    TIME,
    ACTIVITY,
    PURPOSE,
    TOPIC,
    TEXT,
}

/** A searchable recall cue that must cite the evidence from which it was derived. */
data class MemoryAnchor(
    val id: MemoryAnchorId,
    val kind: MemoryAnchorKind,
    val text: MemoryText,
    val evidenceIds: Set<MemoryEvidenceId>,
) {
    init {
        require(evidenceIds.isNotEmpty()) {
            "A memory anchor must cite at least one evidence item."
        }
    }
}

/** A concise memory description that is also tied to source evidence. */
data class MemorySummary(
    val text: MemoryText,
    val evidenceIds: Set<MemoryEvidenceId>,
) {
    init {
        require(evidenceIds.isNotEmpty()) {
            "A memory summary must cite at least one evidence item."
        }
    }
}

/** The searchable semantic shape of a memory. */
data class MemorySignature(
    val summary: MemorySummary,
    val anchors: List<MemoryAnchor>,
) {
    init {
        require(anchors.isNotEmpty()) { "A memory signature needs at least one recall anchor." }
        require(anchors.map(MemoryAnchor::id).distinct().size == anchors.size) {
            "A memory signature cannot contain duplicate anchor IDs."
        }
    }
}

/**
 * The durable, searchable representation of one version of an Asset.
 *
 * A Memory is not the original file. It binds a source identity and source version to
 * a semantic signature whose summary and recall anchors can be explained with stored
 * evidence. The repository will later persist this model after understanding succeeds.
 */
data class Memory(
    val assetIdentity: AssetIdentity,
    val assetFingerprint: AssetFingerprint,
    val signature: MemorySignature,
    val evidence: List<MemoryEvidence>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(evidence.isNotEmpty()) { "A memory must contain supporting evidence." }
        require(evidence.map(MemoryEvidence::id).distinct().size == evidence.size) {
            "A memory cannot contain duplicate evidence IDs."
        }
        require(!updatedAt.isBefore(createdAt)) {
            "A memory cannot be updated before it is created."
        }

        val knownEvidenceIds = evidence.mapTo(mutableSetOf(), MemoryEvidence::id)
        require(signature.summary.evidenceIds.all(knownEvidenceIds::contains)) {
            "A memory summary can only cite evidence stored in the same memory."
        }
        require(signature.anchors.all { anchor ->
            anchor.evidenceIds.all(knownEvidenceIds::contains)
        }) {
            "A memory anchor can only cite evidence stored in the same memory."
        }
    }
}
