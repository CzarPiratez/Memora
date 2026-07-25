package com.memora.app.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.memora.app.domain.asset.SourceId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Schedules unique MediaStore photo/screenshot discovery drain work. */
interface MediaStoreDiscoveryWorkScheduler {
    fun enqueueDrain()

    fun enqueueContinuation()

    fun observeUniqueWork(): Flow<List<WorkInfo>>
}

/**
 * Enqueues unique MediaStore discovery drain work.
 *
 * Metadata placeholders only. Never opens image bytes or schedules OCR.
 */
@Singleton
class DefaultMediaStoreDiscoveryWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : MediaStoreDiscoveryWorkScheduler {
    override fun enqueueDrain() {
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            pageRequest(),
        )
    }

    override fun enqueueContinuation() {
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            pageRequest(),
        )
    }

    override fun observeUniqueWork(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)

    private fun pageRequest() =
        OneTimeWorkRequestBuilder<MediaStoreDiscoveryWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(TAG_MEDIASTORE_DISCOVERY)
            .build()

    companion object {
        const val TAG_MEDIASTORE_DISCOVERY = "mediastore-discovery"
        val SOURCE_ID = SourceId("android-media-store-images")
        val UNIQUE_WORK_NAME = "mediastore-discovery:${SOURCE_ID.value}"
    }
}
