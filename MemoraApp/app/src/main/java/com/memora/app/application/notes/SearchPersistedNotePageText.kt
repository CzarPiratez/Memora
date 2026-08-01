package com.memora.app.application.notes

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Searches already-persisted OneNote page text on this phone.
 *
 * Keyword / substring only. Does not call Microsoft Graph, reopen OneNote,
 * invoke AI, or claim meaning-based Memory recall.
 */
class SearchPersistedNotePageText(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(
        databaseHandle: MemoraDatabaseHandle,
    ) : this(
        database = { databaseHandle.database() },
    )

    constructor(database: MemoraDatabase) : this(database = { database })

    suspend operator fun invoke(rawQuery: String): NotePageKeywordSearchOutcome =
        withContext(Dispatchers.IO) {
            val query = NotePageKeywordSearchSupport.normalizeQuery(rawQuery)
                ?: return@withContext NotePageKeywordSearchOutcome.BlankQuery

            val dao = database().notePageExtractionDao()
            if (dao.countCurrentSearchableNotes(NotePageKeywordSearchSupport.SCHEMA_VERSION) == 0) {
                return@withContext NotePageKeywordSearchOutcome.NothingSavedToSearch(query = query)
            }

            val rows = dao.searchCurrentNoteText(
                escapedNeedle = NotePageKeywordSearchSupport.escapeForLike(query),
                schemaVersion = NotePageKeywordSearchSupport.SCHEMA_VERSION,
                limit = NotePageKeywordSearchSupport.MAX_RESULTS,
            )

            val hits = rows.map { row ->
                NotePageKeywordSearchHit(
                    label = row.displayName?.takeIf { it.isNotBlank() } ?: "OneNote page",
                    excerpt = NotePageKeywordSearchSupport.excerptAroundMatch(
                        row.fullText,
                        query,
                    ),
                    sourceId = row.sourceId,
                    sourceAssetKey = row.sourceAssetKey,
                )
            }

            NotePageKeywordSearchOutcome.Matches(
                query = query,
                hits = hits,
                limitReached = hits.size >= NotePageKeywordSearchSupport.MAX_RESULTS,
            )
        }
}

data class NotePageKeywordSearchHit(
    val label: String,
    val excerpt: String,
    val sourceId: String,
    val sourceAssetKey: String,
) {
    init {
        require(label.isNotBlank()) { "A note search hit needs a label." }
        require(excerpt.isNotBlank()) { "A note search hit needs an excerpt." }
        require(sourceId.isNotBlank()) { "A note search hit needs a source id." }
        require(sourceAssetKey.isNotBlank()) {
            "A note search hit needs a source asset key."
        }
    }
}

sealed interface NotePageKeywordSearchOutcome {
    data object BlankQuery : NotePageKeywordSearchOutcome

    data class NothingSavedToSearch(
        val query: String,
    ) : NotePageKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved outcome needs the submitted search query."
            }
        }
    }

    data class Matches(
        val query: String,
        val hits: List<NotePageKeywordSearchHit>,
        val limitReached: Boolean,
    ) : NotePageKeywordSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "A note keyword search match list needs the query."
            }
            if (limitReached) {
                require(hits.size >= NotePageKeywordSearchSupport.MAX_RESULTS) {
                    "limitReached requires a full result page of " +
                        "${NotePageKeywordSearchSupport.MAX_RESULTS}."
                }
            }
        }
    }
}
