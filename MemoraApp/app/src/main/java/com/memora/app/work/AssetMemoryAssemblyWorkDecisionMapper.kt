package com.memora.app.work

import com.memora.app.application.memory.AssetMemoryDrainResult

internal sealed interface AssetMemoryAssemblyWorkDecision {
    data object Continue : AssetMemoryAssemblyWorkDecision
    data object CompletedDrain : AssetMemoryAssemblyWorkDecision
    data object RetryableFailure : AssetMemoryAssemblyWorkDecision
}

/**
 * Maps one [RunPendingAssetMemoryAssembly] batch onto WorkManager.
 *
 * [AssetMemoryDrainResult.FailedSafely] must not Continue — that is the D-9
 * hot-loop. Retry the same batch after backoff.
 */
internal object AssetMemoryAssemblyWorkDecisionMapper {
    fun map(result: AssetMemoryDrainResult): AssetMemoryAssemblyWorkDecision =
        when (result) {
            is AssetMemoryDrainResult.Completed -> if (result.hasMore) {
                AssetMemoryAssemblyWorkDecision.Continue
            } else {
                AssetMemoryAssemblyWorkDecision.CompletedDrain
            }
            is AssetMemoryDrainResult.FailedSafely ->
                AssetMemoryAssemblyWorkDecision.RetryableFailure
        }
}
