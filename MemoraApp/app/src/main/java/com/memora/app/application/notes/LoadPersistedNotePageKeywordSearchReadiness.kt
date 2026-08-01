package com.memora.app.application.notes

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads how much current-fingerprint OneNote page text is ready for keyword search.
 *
 * Honest inventory only. Does not call Graph, invoke AI, or claim Memory recall.
 */
class LoadPersistedNotePageKeywordSearchReadiness(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(
        databaseHandle: MemoraDatabaseHandle,
    ) : this(
        database = { databaseHandle.database() },
    )

    constructor(database: MemoraDatabase) : this(database = { database })

    suspend operator fun invoke(): NotePageKeywordSearchReadiness =
        withContext(Dispatchers.IO) {
            val counts = database().notePageExtractionDao()
                .countCurrentSearchableCorpus(NotePageKeywordSearchSupport.SCHEMA_VERSION)
            NotePageKeywordSearchReadiness(noteCount = counts.noteCount)
        }
}

data class NotePageKeywordSearchReadiness(
    val noteCount: Int,
) {
    init {
        require(noteCount >= 0) { "Note count cannot be negative." }
    }

    val isEmpty: Boolean get() = noteCount == 0
}
