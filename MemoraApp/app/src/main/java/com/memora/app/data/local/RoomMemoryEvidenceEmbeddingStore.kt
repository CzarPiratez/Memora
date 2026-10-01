package com.memora.app.data.local

import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class RoomMemoryEvidenceEmbeddingStore(
    private val dao: () -> MemoryEvidenceEmbeddingDao,
) : MemoryEvidenceEmbeddingStore {
    override fun find(
        revisionId: MemoryRevisionId,
        evidenceId: MemoryEvidenceId,
        model: ModelVersionIdentity,
    ): MemoryEvidenceEmbeddingRecord? = io {
        dao().find(
            revisionId = revisionId.value,
            evidenceId = evidenceId.value,
            modelId = model.modelId,
            modelVersion = model.version,
        )?.toDomain()
    }

    override fun upsert(record: MemoryEvidenceEmbeddingRecord) {
        io { dao().upsert(record.toEntity()) }
    }

    override fun countForModel(model: ModelVersionIdentity): Int = io {
        dao().countForModel(modelId = model.modelId, modelVersion = model.version)
    }

    override fun listForModel(model: ModelVersionIdentity): List<MemoryEvidenceEmbeddingRecord> = io {
        dao().listForModel(modelId = model.modelId, modelVersion = model.version)
            .map { it.toDomain() }
    }

    override fun deleteForModel(model: ModelVersionIdentity): Int = io {
        dao().deleteForModel(modelId = model.modelId, modelVersion = model.version)
    }

    private fun <T> io(block: suspend () -> T): T =
        runBlocking(Dispatchers.IO) { block() }
}
