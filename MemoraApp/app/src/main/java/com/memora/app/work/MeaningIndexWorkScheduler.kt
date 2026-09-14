package com.memora.app.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface MeaningIndexWorkScheduler {
    fun enqueueDrain()
    fun enqueueContinuation()
    fun cancel()
    fun observeUniqueWork(): Flow<List<WorkInfo>>
}

@Singleton
class DefaultMeaningIndexWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : MeaningIndexWorkScheduler {
    override fun enqueueDrain() {
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request(),
        )
    }

    override fun enqueueContinuation() {
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request(),
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    override fun observeUniqueWork(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)

    private fun request() =
        OneTimeWorkRequestBuilder<MeaningIndexWorker>()
            .setConstraints(Constraints.Builder().build())
            .addTag(TAG_MEANING_INDEX)
            .build()

    companion object {
        const val TAG_MEANING_INDEX = "meaning-index"
        const val UNIQUE_WORK_NAME = "meaning-index-drain"
    }
}
