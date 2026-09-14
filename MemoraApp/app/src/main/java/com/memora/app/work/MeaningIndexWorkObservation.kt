package com.memora.app.work

import androidx.work.WorkInfo

internal data class MeaningIndexWorkTotals(
    val indexedMemories: Int = 0,
    val remainingPending: Int = 0,
    val hasMore: Boolean = false,
    val engineUnavailable: Boolean = false,
    val disagreedPending: Int = 0,
)

internal sealed interface MeaningIndexWorkPhase {
    data object Idle : MeaningIndexWorkPhase
    data class Active(val totals: MeaningIndexWorkTotals) : MeaningIndexWorkPhase
    data class Completed(val totals: MeaningIndexWorkTotals) : MeaningIndexWorkPhase
    data class Cancelled(val totals: MeaningIndexWorkTotals) : MeaningIndexWorkPhase
    data object Failed : MeaningIndexWorkPhase
}

/**
 * Pure mapping of unique-work infos onto UI phase.
 *
 * Progress on the running node is preferred for remaining/indexed-so-far so
 * the card does not wait for a finished batch. Cancelled work is not Failed.
 */
internal object MeaningIndexWorkObservation {
    fun phase(infos: List<WorkInfo>): MeaningIndexWorkPhase {
        if (infos.isEmpty()) return MeaningIndexWorkPhase.Idle
        val totals = totals(infos)
        return when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> MeaningIndexWorkPhase.Active(totals)
            infos.any { it.state == WorkInfo.State.FAILED } ->
                MeaningIndexWorkPhase.Failed
            infos.any { info ->
                info.state == WorkInfo.State.CANCELLED ||
                    info.outputData.getBoolean(MeaningIndexWorker.KEY_STOPPED, false)
            } -> MeaningIndexWorkPhase.Cancelled(totals)
            infos.all { it.state.isFinished } ->
                MeaningIndexWorkPhase.Completed(totals)
            else -> MeaningIndexWorkPhase.Idle
        }
    }

    fun totals(infos: List<WorkInfo>): MeaningIndexWorkTotals {
        val succeeded = infos.filter { it.state == WorkInfo.State.SUCCEEDED }
        val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        val succeededIndexed = succeeded.sumOf {
            it.outputData.getInt(MeaningIndexWorker.KEY_INDEXED, 0)
        }
        val progressIndexed = intOrNull(running?.progress, MeaningIndexWorker.KEY_INDEXED) ?: 0
        val remaining = intOrNull(running?.progress, MeaningIndexWorker.KEY_REMAINING)
            ?: intOrNull(succeeded.lastOrNull()?.outputData, MeaningIndexWorker.KEY_REMAINING)
            ?: 0
        val hasMore = booleanOrNull(running?.progress, MeaningIndexWorker.KEY_HAS_MORE)
            ?: booleanOrNull(succeeded.lastOrNull()?.outputData, MeaningIndexWorker.KEY_HAS_MORE)
            ?: false
        return MeaningIndexWorkTotals(
            indexedMemories = succeededIndexed + progressIndexed,
            remainingPending = remaining,
            hasMore = hasMore,
            engineUnavailable = succeeded.any {
                it.outputData.getBoolean(MeaningIndexWorker.KEY_ENGINE_UNAVAILABLE, false)
            },
            disagreedPending = succeeded.maxOfOrNull {
                it.outputData.getInt(MeaningIndexWorker.KEY_DISAGREED_PENDING, 0)
            } ?: 0,
        )
    }

    private fun intOrNull(data: androidx.work.Data?, key: String): Int? {
        if (data == null || !data.keyValueMap.containsKey(key)) return null
        return data.getInt(key, 0)
    }

    private fun booleanOrNull(data: androidx.work.Data?, key: String): Boolean? {
        if (data == null || !data.keyValueMap.containsKey(key)) return null
        return data.getBoolean(key, false)
    }
}
