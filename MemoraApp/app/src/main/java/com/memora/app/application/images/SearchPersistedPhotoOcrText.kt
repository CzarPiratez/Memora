package com.memora.app.application.images

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchPersistedPhotoOcrText(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(databaseHandle: MemoraDatabaseHandle) : this({ databaseHandle.database() })

    constructor(database: MemoraDatabase) : this({ database })

    suspend operator fun invoke(rawQuery: String): PhotoOcrKeywordSearchOutcome =
        withContext(Dispatchers.IO) {
            val query = PhotoOcrKeywordSearchSupport.normalizeQuery(rawQuery)
                ?: return@withContext PhotoOcrKeywordSearchOutcome.BlankQuery
            val dao = database().photoOcrExtractionDao()
            if (dao.countCurrentSearchablePhotos(PhotoOcrKeywordSearchSupport.SCHEMA_VERSION) == 0) {
                return@withContext PhotoOcrKeywordSearchOutcome.NothingSavedToSearch(query)
            }
            val hits = dao.searchCurrentOcrText(
                PhotoOcrKeywordSearchSupport.escapeForLike(query),
                PhotoOcrKeywordSearchSupport.SCHEMA_VERSION,
                PhotoOcrKeywordSearchSupport.MAX_RESULTS,
            ).map { row ->
                PhotoOcrKeywordSearchHit(
                    label = row.displayName?.takeIf(String::isNotBlank) ?: "Photo",
                    excerpt = PhotoOcrKeywordSearchSupport.excerptAroundMatch(row.fullText, query),
                    sourceId = row.sourceId,
                    sourceAssetKey = row.sourceAssetKey,
                )
            }
            PhotoOcrKeywordSearchOutcome.Matches(
                query,
                hits,
                hits.size >= PhotoOcrKeywordSearchSupport.MAX_RESULTS,
            )
        }
}

data class PhotoOcrKeywordSearchHit(
    val label: String,
    val excerpt: String,
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(label.isNotBlank() && excerpt.isNotBlank())
        require(sourceId.isNotBlank() && sourceAssetKey.isNotBlank())
    }
}

sealed interface PhotoOcrKeywordSearchOutcome {
    data object BlankQuery : PhotoOcrKeywordSearchOutcome
    data class NothingSavedToSearch(val query: String) : PhotoOcrKeywordSearchOutcome
    data class Matches(
        val query: String,
        val hits: List<PhotoOcrKeywordSearchHit>,
        val limitReached: Boolean,
    ) : PhotoOcrKeywordSearchOutcome
}
