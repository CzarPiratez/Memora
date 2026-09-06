package com.memora.app.work

import com.memora.app.application.memory.AssetMemoryDrainResult
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetMemoryAssemblyWorkDecisionMapperTest {
    @Test
    fun completed_with_more_continues() {
        assertEquals(
            AssetMemoryAssemblyWorkDecision.Continue,
            AssetMemoryAssemblyWorkDecisionMapper.map(
                AssetMemoryDrainResult.Completed(
                    assembledCount = 25,
                    skippedCount = 0,
                    currentReadyCount = 25,
                    hasMore = true,
                ),
            ),
        )
    }

    @Test
    fun completed_without_more_finishes() {
        assertEquals(
            AssetMemoryAssemblyWorkDecision.CompletedDrain,
            AssetMemoryAssemblyWorkDecisionMapper.map(
                AssetMemoryDrainResult.Completed(
                    assembledCount = 3,
                    skippedCount = 1,
                    currentReadyCount = 4,
                    hasMore = false,
                ),
            ),
        )
    }

    @Test
    fun failed_safely_retries_and_does_not_continue() {
        assertEquals(
            AssetMemoryAssemblyWorkDecision.RetryableFailure,
            AssetMemoryAssemblyWorkDecisionMapper.map(
                AssetMemoryDrainResult.FailedSafely(
                    assembledCount = 2,
                    skippedCount = 0,
                    currentReadyCount = 2,
                ),
            ),
        )
    }
}
