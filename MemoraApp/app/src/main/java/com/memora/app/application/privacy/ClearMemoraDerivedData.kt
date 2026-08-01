package com.memora.app.application.privacy

import androidx.work.WorkManager
import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.domain.notes.NotesProviderTokenVault
import com.memora.app.work.DefaultMediaStoreDiscoveryWorkScheduler
import com.memora.app.work.DefaultMediaStoreImageExifExtractWorkScheduler
import com.memora.app.work.DefaultMediaStorePhotoOcrExtractWorkScheduler
import com.memora.app.work.DefaultMediaStoreScreenshotOcrExtractWorkScheduler
import com.memora.app.work.DefaultOneNotePageExtractWorkScheduler
import com.memora.app.work.DefaultSafPdfDiscoveryWorkScheduler
import com.memora.app.work.DefaultSafPdfExtractWorkScheduler
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * User-confirmed clearing of Memora-owned derived index state only.
 * Recovery copy never mentions SQLCipher, keys, or encryption failures (ADR-021).
 *
 * Also cancels Memora WorkManager jobs so reconnect cannot treat prior finished
 * unique work as a fresh indexing/reading result.
 */
class ClearMemoraDerivedData @Inject constructor(
    private val databaseHandle: MemoraDatabaseHandle,
    private val workManager: WorkManager,
    private val notesProviderTokenVault: NotesProviderTokenVault,
    private val oneNoteAuth: OneNoteInteractiveAuth,
) {
    operator fun invoke(): ClearMemoraDerivedDataResult = try {
        workManager.cancelAllWorkByTag(DefaultSafPdfDiscoveryWorkScheduler.TAG_SAF_PDF_DISCOVERY)
        workManager.cancelAllWorkByTag(DefaultSafPdfExtractWorkScheduler.TAG_SAF_PDF_EXTRACT)
        workManager.cancelAllWorkByTag(DefaultMediaStoreDiscoveryWorkScheduler.TAG_MEDIASTORE_DISCOVERY)
        workManager.cancelAllWorkByTag(
            DefaultMediaStoreImageExifExtractWorkScheduler.TAG_MEDIASTORE_IMAGE_EXIF_EXTRACT,
        )
        workManager.cancelAllWorkByTag(
            DefaultMediaStoreScreenshotOcrExtractWorkScheduler.TAG_MEDIASTORE_SCREENSHOT_OCR_EXTRACT,
        )
        workManager.cancelAllWorkByTag(
            DefaultMediaStorePhotoOcrExtractWorkScheduler.TAG_MEDIASTORE_PHOTO_OCR_EXTRACT,
        )
        workManager.cancelAllWorkByTag(
            DefaultOneNotePageExtractWorkScheduler.TAG_ONENOTE_PAGE_EXTRACT,
        )
        runBlocking {
            oneNoteAuth.disconnect()
        }
        notesProviderTokenVault.clearSession()
        databaseHandle.clearUserConfirmedDerivedData()
        ClearMemoraDerivedDataResult.Cleared(APPROVED_REBUILD_MESSAGE)
    } catch (_: Exception) {
        ClearMemoraDerivedDataResult.Failed
    }

    companion object {
        const val APPROVED_REBUILD_MESSAGE =
            "Your private Memora index needs to be rebuilt. Your original photos, documents, and notes are unchanged."
    }
}

sealed interface ClearMemoraDerivedDataResult {
    data class Cleared(val message: String) : ClearMemoraDerivedDataResult

    data object Failed : ClearMemoraDerivedDataResult
}
