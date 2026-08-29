package com.memora.app.domain.intelligence

import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId

/**
 * Versioned dense vector for Memory / query embedding (Spec §4 EmbeddingEngine).
 *
 * Values are finite floats. This type alone never implies product AVAILABLE.
 */
data class EmbeddingVector(
    val values: FloatArray,
) {
    val dimensions: Int get() = values.size

    init {
        require(values.isNotEmpty()) { "An embedding vector needs at least one dimension." }
        require(values.all { it.isFinite() }) {
            "Embedding vector values must be finite."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingVector) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()
}

sealed interface EmbeddingEncodeResult {
    data class Success(
        val vector: EmbeddingVector,
        val model: ModelVersionIdentity,
    ) : EmbeddingEncodeResult

    data class Unavailable(
        val reason: String,
    ) : EmbeddingEncodeResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Failed(
        val reason: String,
    ) : EmbeddingEncodeResult {
        init {
            require(reason.isNotBlank())
        }
    }
}

/**
 * One persisted Memory embedding revision, keyed by Memory revision + model.
 */
data class MemoryEmbeddingRecord(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val model: ModelVersionIdentity,
    val vector: EmbeddingVector,
    val sourceTextFingerprint: String,
    val createdAtEpochMs: Long,
) {
    init {
        require(sourceTextFingerprint.isNotBlank()) {
            "A memory embedding needs a non-blank source-text fingerprint."
        }
        require(createdAtEpochMs >= 0)
    }
}

/** Pure cosine similarity for candidate ranking tests (not a product recall UI). */
object EmbeddingSimilarity {
    fun cosine(a: EmbeddingVector, b: EmbeddingVector): Float {
        require(a.dimensions == b.dimensions) {
            "Cosine similarity requires equal embedding dimensions."
        }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (index in a.values.indices) {
            val av = a.values[index].toDouble()
            val bv = b.values[index].toDouble()
            dot += av * bv
            normA += av * av
            normB += bv * bv
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))).toFloat()
    }
}

interface MemoryEmbeddingStore {
    fun find(revisionId: MemoryRevisionId, model: ModelVersionIdentity): MemoryEmbeddingRecord?

    fun upsert(record: MemoryEmbeddingRecord)

    fun countForModel(model: ModelVersionIdentity): Int

    /** All vectors for one model identity (candidate recall drain). */
    fun listForModel(model: ModelVersionIdentity): List<MemoryEmbeddingRecord>
}

/**
 * One persisted PDF page embedding, keyed by Memory revision + page + model.
 *
 * Used for E5c Find-by-meaning page ranking. Distinct from summary-only
 * [MemoryEmbeddingRecord].
 */
data class PdfPageEmbeddingRecord(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val pageNumber: Int,
    val model: ModelVersionIdentity,
    val vector: EmbeddingVector,
    val sourceTextFingerprint: String,
    val createdAtEpochMs: Long,
) {
    init {
        require(pageNumber > 0)
        require(sourceTextFingerprint.isNotBlank())
        require(createdAtEpochMs >= 0)
    }
}

interface PdfPageEmbeddingStore {
    fun find(
        revisionId: MemoryRevisionId,
        pageNumber: Int,
        model: ModelVersionIdentity,
    ): PdfPageEmbeddingRecord?

    fun upsert(record: PdfPageEmbeddingRecord)

    fun countForModel(model: ModelVersionIdentity): Int

    fun listForModel(model: ModelVersionIdentity): List<PdfPageEmbeddingRecord>
}

/**
 * One persisted evidence-level embedding, keyed by Memory revision + evidence + model.
 *
 * MIG-05 dual-store interim: [IndexPdfPageEmbeddings] dual-writes PDF page
 * vectors here when [MemoryEvidenceId] resolves. Distinct from summary-level
 * [MemoryEmbeddingRecord]. After MIG-05 step 3, production Search ranks
 * page/evidence hits from this store + [com.memora.app.domain.memory.MemoryEvidence]
 * (not [PdfPageEmbeddingStore]). [PdfPageEmbeddingRecord] remains written until
 * a later retirement step.
 */
data class MemoryEvidenceEmbeddingRecord(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val evidenceId: MemoryEvidenceId,
    val model: ModelVersionIdentity,
    val vector: EmbeddingVector,
    val sourceTextFingerprint: String,
    val createdAtEpochMs: Long,
) {
    init {
        require(sourceTextFingerprint.isNotBlank()) {
            "An evidence embedding needs a non-blank source-text fingerprint."
        }
        require(createdAtEpochMs >= 0)
    }
}

interface MemoryEvidenceEmbeddingStore {
    fun find(
        revisionId: MemoryRevisionId,
        evidenceId: MemoryEvidenceId,
        model: ModelVersionIdentity,
    ): MemoryEvidenceEmbeddingRecord?

    fun upsert(record: MemoryEvidenceEmbeddingRecord)

    fun countForModel(model: ModelVersionIdentity): Int

    fun listForModel(model: ModelVersionIdentity): List<MemoryEvidenceEmbeddingRecord>
}
