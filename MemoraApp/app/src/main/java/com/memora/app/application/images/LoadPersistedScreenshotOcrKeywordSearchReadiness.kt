package com.memora.app.application.images

import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads how much current-fingerprint screenshot OCR text is ready for keyword search.
 *
 * Honest inventory only. Does not reopen images, invoke AI, or claim Memory recall.
 */
class LoadPersistedScreenshotOcrKeywordSearchReadiness(
    private val database: () -> MemoraDatabase,
) {
    @Inject
    constructor(
        databaseHandle: MemoraDatabaseHandle,
    ) : this(
        database = { databaseHandle.database() },
    )

    constructor(database: MemoraDatabase) : this(database = { database })

    suspend operator fun invoke(): ScreenshotOcrKeywordSearchReadiness =
        withContext(Dispatchers.IO) {
            val counts = database().screenshotOcrExtractionDao()
                .countCurrentSearchableCorpus(ScreenshotOcrKeywordSearchSupport.SCHEMA_VERSION)
            ScreenshotOcrKeywordSearchReadiness(screenshotCount = counts.screenshotCount)
        }
}

data class ScreenshotOcrKeywordSearchReadiness(
    val screenshotCount: Int,
) {
    init {
        require(screenshotCount >= 0) { "Screenshot count cannot be negative." }
    }

    val isEmpty: Boolean get() = screenshotCount == 0
}
