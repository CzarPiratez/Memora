package com.memora.app.work

import androidx.work.WorkInfo

internal data class AssetMemoryAssemblyWorkTotals(
    val assembled: Int = 0,
    val skipped: Int = 0,
    val readyCount: Int = 0,
    val hasMore: Boolean = false,
)

internal sealed interface AssetMemoryAssemblyWorkPhase {
    data object Idle : AssetMemoryAssemblyWorkPhase
    data class Active(val totals: AssetMemoryAssemblyWorkTotals) : AssetMemoryAssemblyWorkPhase
    data class Completed(val totals: AssetMemoryAssemblyWorkTotals) : AssetMemoryAssemblyWorkPhase
    data class Cancelled(val totals: AssetMemoryAssemblyWorkTotals) : AssetMemoryAssemblyWorkPhase
    data object Failed : AssetMemoryAssemblyWorkPhase
}

/**
 * Pure mapping of unique-work infos onto UI phase.
 *
 * Cancelled work is not Failed — the user asked to stop. FailedSafely retries
 * inside the worker; a visible Failed row is only a terminal WorkManager failure.
 */
internal object AssetMemoryAssemblyWorkObservation {
    fun phase(infos: List<WorkInfo>): AssetMemoryAssemblyWorkPhase {
        if (infos.isEmpty()) return AssetMemoryAssemblyWorkPhase.Idle
        val totals = totals(infos)
        return when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> AssetMemoryAssemblyWorkPhase.Active(totals)
            infos.any { it.state == WorkInfo.State.FAILED } ->
                AssetMemoryAssemblyWorkPhase.Failed
            infos.any { it.state == WorkInfo.State.CANCELLED } ->
                AssetMemoryAssemblyWorkPhase.Cancelled(totals)
            infos.all { it.state.isFinished } ->
                AssetMemoryAssemblyWorkPhase.Completed(totals)
            else -> AssetMemoryAssemblyWorkPhase.Idle
        }
    }

    fun totals(infos: List<WorkInfo>): AssetMemoryAssemblyWorkTotals {
        val succeeded = infos.filter { it.state == WorkInfo.State.SUCCEEDED }
        val last = succeeded.lastOrNull()?.outputData
        return AssetMemoryAssemblyWorkTotals(
            assembled = succeeded.sumOf {
                it.outputData.getInt(AssetMemoryAssemblyWorker.KEY_ASSEMBLED, 0)
            },
            skipped = succeeded.sumOf {
                it.outputData.getInt(AssetMemoryAssemblyWorker.KEY_SKIPPED, 0)
            },
            readyCount = last?.getInt(AssetMemoryAssemblyWorker.KEY_READY_COUNT, 0) ?: 0,
            hasMore = last?.getBoolean(AssetMemoryAssemblyWorker.KEY_HAS_MORE, false) ?: false,
        )
    }
}
