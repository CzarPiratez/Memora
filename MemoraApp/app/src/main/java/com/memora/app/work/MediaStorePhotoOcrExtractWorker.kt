package com.memora.app.work

import android.content.Context
import android.os.CancellationSignal
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.memora.app.application.images.PendingPhotoOcrExtractor
import com.memora.app.domain.asset.SourceId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MediaStorePhotoOcrExtractWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runPendingExtract: PendingPhotoOcrExtractor,
    private val scheduler: MediaStorePhotoOcrExtractWorkScheduler,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val sourceIdValue = inputData.getString(KEY_SOURCE_ID)?.takeIf(String::isNotBlank)
            ?: return Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_INVALID_INPUT))
        val sourceId = try {
            SourceId(sourceIdValue)
        } catch (_: IllegalArgumentException) {
            return Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_INVALID_INPUT))
        }
        val cancellationSignal = CancellationSignal()
        if (isStopped) {
            cancellationSignal.cancel()
            return Result.retry()
        }
        val outcome = try {
            runPendingExtract(
                sourceId,
                inputData.getString(KEY_AFTER_SOURCE_ASSET_KEY)?.takeIf(String::isNotBlank),
                cancellationSignal,
            )
        } catch (_: Exception) {
            return Result.retry()
        }
        return when (val decision = MediaStorePhotoOcrExtractWorkDecisionMapper.map(outcome)) {
            is MediaStorePhotoOcrExtractWorkDecision.Continue -> {
                scheduler.enqueueContinuation(sourceId, decision.afterSourceAssetKey)
                Result.success(workDataOf(KEY_HAS_MORE to true))
            }
            MediaStorePhotoOcrExtractWorkDecision.CompletedDrain ->
                Result.success(workDataOf(KEY_HAS_MORE to false))
            MediaStorePhotoOcrExtractWorkDecision.AccessStopped ->
                Result.failure(workDataOf(KEY_FAILURE_REASON to REASON_ACCESS_STOPPED))
            MediaStorePhotoOcrExtractWorkDecision.RetryableFailure -> Result.retry()
        }
    }

    companion object {
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_AFTER_SOURCE_ASSET_KEY = "after_source_asset_key"
        const val KEY_HAS_MORE = "has_more"
        const val KEY_FAILURE_REASON = "failure_reason"
        const val REASON_ACCESS_STOPPED = "access_stopped"
        const val REASON_INVALID_INPUT = "invalid_input"
    }
}
