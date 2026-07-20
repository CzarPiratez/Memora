package com.memora.app.ui.setup

import com.memora.app.application.documents.DocumentTreeApprover
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.DocumentTreeSource
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentTreeSetupViewModelTest {
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
    fun staysIdleUntilTheActivityReportsPersistedReadAccess() = runTest {
        val approver = RecordingApprover()
        val viewModel = DocumentTreeSetupViewModel(approver)

        assertEquals(DocumentTreeSetupUiState(), viewModel.uiState.value)
        assertEquals(0, approver.invocationCount)
    }

    @Test
    fun savesOnlyTheUriReportedAfterAndroidPersistsReadAccess() = runTest {
        val approver = RecordingApprover()
        val viewModel = DocumentTreeSetupViewModel(approver)
        val treeUri = "content://example/tree/documents"

        viewModel.onPersistedReadAccessReceived(treeUri)
        assertEquals(DocumentTreeConnectionState.SAVING, viewModel.uiState.value.connection)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(treeUri), approver.receivedUris)
        assertEquals(DocumentTreeConnectionState.CONNECTED, viewModel.uiState.value.connection)
    }

    @Test
    fun reportsAPersistableGrantFailureWithoutSavingAnything() = runTest {
        val approver = RecordingApprover()
        val viewModel = DocumentTreeSetupViewModel(approver)

        viewModel.onPersistableReadAccessFailed()

        assertEquals(0, approver.invocationCount)
        assertEquals(
            DocumentTreeConnectionState.FAILED(
                "Android could not retain read access to this folder. Please choose it again.",
            ),
            viewModel.uiState.value.connection,
        )
    }

    @Test
    fun reportsASafeRetryMessageWhenSavingThePrivateReferenceFails() = runTest {
        val viewModel = DocumentTreeSetupViewModel(RecordingApprover(shouldFail = true))

        viewModel.onPersistedReadAccessReceived("content://example/tree/documents")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            DocumentTreeConnectionState.FAILED(
                "Memora could not save this folder connection. Please choose it again.",
            ),
            viewModel.uiState.value.connection,
        )
    }

    private class RecordingApprover(
        private val shouldFail: Boolean = false,
    ) : DocumentTreeApprover {
        var invocationCount: Int = 0
        val receivedUris = mutableListOf<String>()

        override suspend fun invoke(persistedTreeUri: String): DocumentTreeApproval {
            invocationCount += 1
            receivedUris += persistedTreeUri
            if (shouldFail) error("database unavailable")

            return DocumentTreeSource.approvalFor(
                persistedTreeUri = persistedTreeUri,
                approvedAt = Instant.parse("2026-07-20T12:00:00Z"),
            )
        }
    }
}
