package com.memora.app.ui.setup

import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackContainer
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.AiPackPayloadStore
import com.memora.app.domain.intelligence.AiPackPayloadVerifier
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
    fun acknowledge_then_activate_stores_container_without_claiming_meaning_search() = runTest {
        val ledger = InMemoryAiPackInstallLedger()
        val store = InMemoryStore()
        val viewModel = AiPackDisclosureViewModel(
            ledger = ledger,
            aiPackManager = LedgerBackedAiPackManager(ledger),
            activateOfflinePack = ActivateOfflineEmbeddingPackContainer(
                ledger = ledger,
                payloadStore = store,
                verifier = AiPackPayloadVerifier(),
            ),
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showAcknowledge)
        assertFalse(viewModel.uiState.value.showActivate)

        viewModel.onAcknowledgeRequested()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showActivate)

        viewModel.onActivateRequested()
        advanceUntilIdle()

        assertEquals(
            AiPackInstallState.ACTIVE,
            ledger.entry(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)?.installationState,
        )
        assertFalse(viewModel.uiState.value.showActivate)
        assertTrue(
            viewModel.uiState.value.statusBody.contains("does not claim", ignoreCase = true),
        )
        assertEquals(
            AiPackDisclosureCopy.FEEDBACK_ACTIVATED,
            viewModel.uiState.value.feedbackMessage,
        )
    }

    private class InMemoryStore : AiPackPayloadStore {
        private val map = linkedMapOf<String, ByteArray>()
        override fun writePayload(packId: String, payload: ByteArray) {
            map[packId] = payload.copyOf()
        }

        override fun readPayload(packId: String): ByteArray? = map[packId]?.copyOf()

        override fun deletePayload(packId: String) {
            map.remove(packId)
        }

        override fun clearAll() {
            map.clear()
        }
    }
}
