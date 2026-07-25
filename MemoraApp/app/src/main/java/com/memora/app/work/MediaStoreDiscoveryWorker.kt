package com.memora.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memora.app.application.discovery.MediaStoreImageIndexer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs one bounded MediaStore photo/screenshot metadata discovery page.
 *
 * Never opens image bytes, runs OCR, or invokes AI.
 */
@HiltWorker
class MediaStoreDiscoveryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val indexMediaStoreImages: MediaStoreImageIndexer,
    private val scheduler: MediaStoreDiscoveryWorkScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val outcome = try {
            indexMediaStoreImages()
        } catch (_: Exception) {
            return Result.retry()
        }

        return when (val decision = MediaStoreDiscoveryWorkDecisionMapper.map(outcome)) {
            is MediaStoreDiscoveryWorkDecision.Continue -> {
                scheduler.enqueueContinuation()
                Result.success(
                    workDataOf(
                        KEY_PAGE_ASSET_COUNT to decision.pageAssetCount,
                        KEY_HAS_MORE to true,
                        KEY_ACCESS_SCOPE to decision.accessScope,
                    ),
                )
            }

            is MediaStoreDiscoveryWorkDecision.Completed -> Result.success(
                workDataOf(
                    KEY_PAGE_ASSET_COUNT to decision.pageAssetCount,
                    KEY_HAS_MORE to false,
                    KEY_ACCESS_SCOPE to decision.accessScope,
                ),
            )

            MediaStoreDiscoveryWorkDecision.AccessStopped -> Result.failure(
                workDataOf(KEY_FAILURE_REASON to REASON_ACCESS_STOPPED),
            )

            is MediaStoreDiscoveryWorkDecision.RetryableFailure -> Result.retry()
        }
    }

    companion object {
        const val KEY_PAGE_ASSET_COUNT = "page_asset_count"
        const val KEY_HAS_MORE = "has_more"
        const val KEY_ACCESS_SCOPE = "access_scope"
        const val KEY_FAILURE_REASON = "failure_reason"
        const val REASON_ACCESS_STOPPED = "access_stopped"
    }
}
