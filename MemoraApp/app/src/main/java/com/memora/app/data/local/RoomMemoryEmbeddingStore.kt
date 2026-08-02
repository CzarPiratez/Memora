package com.memora.app.data.local

import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class RoomMemoryEmbeddingStore(
    private val dao: () -> MemoryEmbeddingDao,
) : MemoryEmbeddingStore {
    override fun find(
        revisionId: MemoryRevisionId,
        model: ModelVersionIdentity,
    ): MemoryEmbeddingRecord? = io {
        dao().find(
            revisionId = revisionId.value,
            modelId = model.modelId,
            modelVersion = model.version,
        )?.toDomain()
    }

    override fun upsert(record: MemoryEmbeddingRecord) {
        io { dao().upsert(record.toEntity()) }
    }

    override fun countForModel(model: ModelVersionIdentity): Int = io {
        dao().countForModel(modelId = model.modelId, modelVersion = model.version)
    }

    private fun <T> io(block: suspend () -> T): T =
        runBlocking(Dispatchers.IO) { block() }
}
