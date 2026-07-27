package com.memora.app.application.documents

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads how much current-fingerprint PDF page text is ready for keyword search.
 *
 * Honest inventory only. Does not reopen PDFs, invoke AI, or claim Memory recall.
 */
class LoadPersistedPdfKeywordSearchReadiness(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(
        databaseHandle: MemoraDatabaseHandle,
    ) : this(
        database = { databaseHandle.database() },
    )

    /** Test helper bound to one in-memory / fixture database. */
    constructor(database: MemoraDatabase) : this(database = { database })

    suspend operator fun invoke(): PdfKeywordSearchReadiness =
        withContext(Dispatchers.IO) {
            val counts = database().pdfExtractionDao()
                .countCurrentSearchableCorpus(PdfKeywordSearchSupport.SCHEMA_VERSION)
            PdfKeywordSearchReadiness(
                pageCount = counts.pageCount,
                documentCount = counts.documentCount,
            )
        }
}

data class PdfKeywordSearchReadiness(
    val pageCount: Int,
    val documentCount: Int,
) {
    init {
        require(pageCount >= 0) { "Page count cannot be negative." }
        require(documentCount >= 0) { "Document count cannot be negative." }
        if (pageCount == 0) {
            require(documentCount == 0) {
                "Empty page corpus cannot report documents."
            }
        } else {
            require(documentCount > 0) {
                "Non-empty page corpus needs at least one document."
            }
        }
    }

    val isEmpty: Boolean get() = pageCount == 0
}
