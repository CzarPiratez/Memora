package com.memora.app.application.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningIndexDrainBudgetTest {
    @Test
    fun count_cap_stays_twenty_five_and_worker_budget_is_four_minutes() {
        assertEquals(25, MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP)
        assertEquals(240_000L, MeaningIndexBatchLimits.WORKER_WALL_CLOCK_BUDGET_MS)
    }

    @Test
    fun first_batch_always_fits() {
        assertTrue(
            MeaningIndexDrainBudget.anotherBatchFits(
                startedAtMs = 0,
                nowMs = 10_000,
                batchesAlreadyRun = 0,
                budgetMs = 1,
            ),
        )
    }

    @Test
    fun later_batch_fits_inside_budget() {
        assertTrue(
            MeaningIndexDrainBudget.anotherBatchFits(
                startedAtMs = 100,
                nowMs = 199,
                batchesAlreadyRun = 3,
                budgetMs = 100,
            ),
        )
    }

    @Test
    fun later_batch_stops_at_budget() {
        assertFalse(
            MeaningIndexDrainBudget.anotherBatchFits(
                startedAtMs = 100,
                nowMs = 200,
                batchesAlreadyRun = 1,
                budgetMs = 100,
            ),
        )
    }
}
