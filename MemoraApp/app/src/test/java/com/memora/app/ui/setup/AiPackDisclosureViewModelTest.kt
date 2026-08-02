package com.memora.app.ui.setup

import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.InMemoryAiPackInstallLedger
import com.memora.app.domain.intelligence.LedgerBackedAiPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiPackDisclosureViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun acknowledge_records_disclosure_without_activating_pack() = runTest {
        val ledger = InMemoryAiPackInstallLedger()
        val viewModel = AiPackDisclosureViewModel(
            ledger = ledger,
            aiPackManager = LedgerBackedAiPackManager(ledger),
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showAcknowledge)

        viewModel.onAcknowledgeRequested()
        advanceUntilIdle()

        val entry = ledger.entry(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
        assertTrue(entry?.disclosureAcknowledgedAtEpochMs != null)
        assertEquals(AiPackInstallState.NOT_INSTALLED, entry?.installationState)
        assertFalse(viewModel.uiState.value.showAcknowledge)
        assertTrue(
            viewModel.uiState.value.statusBody.contains("not installed", ignoreCase = true),
        )
        assertEquals(
            AiPackDisclosureCopy.FEEDBACK_ACKNOWLEDGED,
            viewModel.uiState.value.feedbackMessage,
        )
    }
}
