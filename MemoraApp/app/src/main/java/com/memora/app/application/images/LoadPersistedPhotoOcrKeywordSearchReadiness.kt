package com.memora.app.application.images

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoadPersistedPhotoOcrKeywordSearchReadiness(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(databaseHandle: MemoraDatabaseHandle) : this({ databaseHandle.database() })

    constructor(database: MemoraDatabase) : this({ database })

    suspend operator fun invoke(): PhotoOcrKeywordSearchReadiness = withContext(Dispatchers.IO) {
        val counts = database().photoOcrExtractionDao()
            .countCurrentSearchableCorpus(PhotoOcrKeywordSearchSupport.SCHEMA_VERSION)
        PhotoOcrKeywordSearchReadiness(counts.photoCount)
    }
}

data class PhotoOcrKeywordSearchReadiness(val photoCount: Int) {
    init {
        require(photoCount >= 0)
    }
    val isEmpty get() = photoCount == 0
}
