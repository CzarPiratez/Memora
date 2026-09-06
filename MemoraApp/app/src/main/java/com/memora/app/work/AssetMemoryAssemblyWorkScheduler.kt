package com.memora.app.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface AssetMemoryAssemblyWorkScheduler {
    fun enqueueDrain()
    fun enqueueContinuation()
    fun cancel()
    fun observeUniqueWork(): Flow<List<WorkInfo>>
}

@Singleton
class DefaultAssetMemoryAssemblyWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) : AssetMemoryAssemblyWorkScheduler {
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
        OneTimeWorkRequestBuilder<AssetMemoryAssemblyWorker>()
            .setConstraints(Constraints.Builder().build())
            .addTag(TAG_ASSET_MEMORY_ASSEMBLY)
            .build()

    companion object {
        const val TAG_ASSET_MEMORY_ASSEMBLY = "asset-memory-assembly"
        const val UNIQUE_WORK_NAME = "asset-memory-assembly-drain"
    }
}
