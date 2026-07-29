package com.memora.app.application.images

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Searches already-persisted screenshot OCR text on this phone.
 *
 * Keyword / substring only. Does not reopen images, invoke AI, use the network,
 * or claim meaning-based Memory recall.
 */
class SearchPersistedScreenshotOcrText(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(
        databaseHandle: MemoraDatabaseHandle,
    ) : this(
        database = { databaseHandle.database() },
    )

    constructor(database: MemoraDatabase) : this(database = { database })

    suspend operator fun invoke(rawQuery: String): ScreenshotOcrKeywordSearchOutcome =
        withContext(Dispatchers.IO) {
            val query = ScreenshotOcrKeywordSearchSupport.normalizeQuery(rawQuery)
                ?: return@withContext ScreenshotOcrKeywordSearchOutcome.BlankQuery

            val dao = database().screenshotOcrExtractionDao()
            if (dao.countCurrentSearchableScreenshots(
                    ScreenshotOcrKeywordSearchSupport.SCHEMA_VERSION,
                ) == 0
            ) {
                return@withContext ScreenshotOcrKeywordSearchOutcome.NothingSavedToSearch(
                    query = query,
                )
            }

            val rows = dao.searchCurrentOcrText(
                escapedNeedle = ScreenshotOcrKeywordSearchSupport.escapeForLike(query),
                schemaVersion = ScreenshotOcrKeywordSearchSupport.SCHEMA_VERSION,
                limit = ScreenshotOcrKeywordSearchSupport.MAX_RESULTS,
            )

            val hits = rows.map { row ->
                ScreenshotOcrKeywordSearchHit(
                    label = row.displayName?.takeIf { it.isNotBlank() } ?: "Screenshot",
                    excerpt = ScreenshotOcrKeywordSearchSupport.excerptAroundMatch(
                        row.fullText,
                        query,
                    ),
                    sourceId = row.sourceId,
                    sourceAssetKey = row.sourceAssetKey,
                )
            }

            ScreenshotOcrKeywordSearchOutcome.Matches(
                query = query,
                hits = hits,
                limitReached = hits.size >= ScreenshotOcrKeywordSearchSupport.MAX_RESULTS,
            )
        }
}

data class ScreenshotOcrKeywordSearchHit(
    val label: String,
    val excerpt: String,
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(label.isNotBlank()) { "A screenshot OCR search hit needs a label." }
        require(excerpt.isNotBlank()) { "A screenshot OCR search hit needs an excerpt." }
        require(sourceId.isNotBlank()) { "A screenshot OCR search hit needs a source id." }
        require(sourceAssetKey.isNotBlank()) {
            "A screenshot OCR search hit needs a source asset key."
        }
    }
}

sealed interface ScreenshotOcrKeywordSearchOutcome {
    data object BlankQuery : ScreenshotOcrKeywordSearchOutcome

    data class NothingSavedToSearch(
        val query: String,
    ) : ScreenshotOcrKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved outcome needs the submitted search query."
            }
        }
    }

    data class Matches(
        val query: String,
        val hits: List<ScreenshotOcrKeywordSearchHit>,
        val limitReached: Boolean,
    ) : ScreenshotOcrKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "A screenshot OCR keyword search match list needs the query."
            }
            if (limitReached) {
                require(hits.size >= ScreenshotOcrKeywordSearchSupport.MAX_RESULTS) {
                    "limitReached requires a full result page of " +
                        "${ScreenshotOcrKeywordSearchSupport.MAX_RESULTS}."
                }
            }
        }
    }
}
