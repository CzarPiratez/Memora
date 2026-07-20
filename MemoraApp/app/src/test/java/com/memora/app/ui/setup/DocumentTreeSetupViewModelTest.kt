package com.memora.app.ui.setup

import com.memora.app.application.documents.DocumentTreeApprover
import com.memora.app.application.documents.PdfFolderConnectionFinder
import com.memora.app.application.documents.SafPdfFolderIndexer
import com.memora.app.application.documents.SafPdfFolderIndexingOutcome
import com.memora.app.domain.asset.SourceId
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
    fun restoresNoSavedFolderToTheReadyStateWithoutReadingASource() = runTest {
        val approver = RecordingApprover()
        val finder = RecordingFinder()
        val viewModel = DocumentTreeSetupViewModel(approver, finder, RecordingIndexer())

        assertEquals(DocumentTreeConnectionState.LOADING, viewModel.uiState.value.connection)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(DocumentTreeConnectionState.READY, viewModel.uiState.value.connection)
        assertEquals(0, approver.invocationCount)
        assertEquals(1, finder.invocationCount)
    }

    @Test
    fun restoresTheMostRecentSavedFolderForAnExplicitIndexingRequest() = runTest {
        val sourceId = SourceId("android-saf-document-tree:restored")
        val indexer = RecordingIndexer(
            outcome = SafPdfFolderIndexingOutcome.Indexed(
                sourceId = sourceId,
                discoveredAssetCount = 1,
                hasMore = false,
            ),
        )
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(sourceId = sourceId),
            indexer,
        )

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(DocumentTreeConnectionState.CONNECTED(sourceId), viewModel.uiState.value.connection)

        viewModel.onIndexRequested()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(sourceId), indexer.receivedSourceIds)
        assertEquals(
            PdfFolderIndexingState.COMPLETED(discoveredAssetCount = 1, hasMore = false),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun savesOnlyTheUriReportedAfterAndroidPersistsReadAccess() = runTest {
        val approver = RecordingApprover()
        val viewModel = DocumentTreeSetupViewModel(approver, RecordingFinder(), RecordingIndexer())
        val treeUri = "content://example/tree/documents"

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPersistedReadAccessReceived(treeUri)
        assertEquals(DocumentTreeConnectionState.SAVING, viewModel.uiState.value.connection)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(treeUri), approver.receivedUris)
        assertEquals(
            DocumentTreeConnectionState.CONNECTED(DocumentTreeSource.sourceIdFor(treeUri)),
            viewModel.uiState.value.connection,
        )
    }

    @Test
    fun reportsAPersistableGrantFailureWithoutSavingAnything() = runTest {
        val approver = RecordingApprover()
        val viewModel = DocumentTreeSetupViewModel(approver, RecordingFinder(), RecordingIndexer())

        dispatcher.scheduler.advanceUntilIdle()

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
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(shouldFail = true),
            RecordingFinder(),
            RecordingIndexer(),
        )

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPersistedReadAccessReceived("content://example/tree/documents")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            DocumentTreeConnectionState.FAILED(
                "Memora could not save this folder connection. Please choose it again.",
            ),
            viewModel.uiState.value.connection,
        )
    }

    @Test
    fun indexesOnlyAfterTheUserConnectedAFolderAndExplicitlyRequestsIt() = runTest {
        val treeUri = "content://example/tree/documents"
        val sourceId = DocumentTreeSource.sourceIdFor(treeUri)
        val indexer = RecordingIndexer(
            outcome = SafPdfFolderIndexingOutcome.Indexed(
                sourceId = sourceId,
                discoveredAssetCount = 2,
                hasMore = true,
            ),
        )
        val viewModel = DocumentTreeSetupViewModel(RecordingApprover(), RecordingFinder(), indexer)

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIndexRequested()
        assertEquals(emptyList<SourceId>(), indexer.receivedSourceIds)

        viewModel.onPersistedReadAccessReceived(treeUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIndexRequested()
        assertEquals(PdfFolderIndexingState.IN_PROGRESS, viewModel.uiState.value.indexing)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(sourceId), indexer.receivedSourceIds)
        assertEquals(
            PdfFolderIndexingState.COMPLETED(discoveredAssetCount = 2, hasMore = true),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun reportsRevokedFolderAccessAsAConnectionRecoveryState() = runTest {
        val treeUri = "content://example/tree/documents"
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(),
            RecordingIndexer(outcome = SafPdfFolderIndexingOutcome.AccessRevoked),
        )

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPersistedReadAccessReceived(treeUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIndexRequested()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            DocumentTreeConnectionState.FAILED(
                "Android no longer allows Memora to read this folder. Please choose it again.",
            ),
            viewModel.uiState.value.connection,
        )
    }

    @Test
    fun exposesProviderFailureForAnExplicitRetryWithoutDroppingTheConnection() = runTest {
        val treeUri = "content://example/tree/documents"
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(),
            RecordingIndexer(
                outcome = SafPdfFolderIndexingOutcome.Failed(
                    com.memora.app.domain.discovery.DiscoveryFailure(
                        code = "SAF_DOCUMENT_QUERY_FAILED",
                        message = "Memora could not read PDF metadata from the approved folder. You can retry later.",
                    ),
                ),
            ),
        )

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPersistedReadAccessReceived(treeUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIndexRequested()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            PdfFolderIndexingState.FAILED(
                "Memora could not read PDF metadata from the approved folder. You can retry later.",
            ),
            viewModel.uiState.value.indexing,
        )
        assertEquals(
            DocumentTreeConnectionState.CONNECTED(DocumentTreeSource.sourceIdFor(treeUri)),
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

    private class RecordingFinder(
        private val sourceId: SourceId? = null,
        private val shouldFail: Boolean = false,
    ) : PdfFolderConnectionFinder {
        var invocationCount: Int = 0

        override suspend fun invoke(): SourceId? {
            invocationCount += 1
            if (shouldFail) error("database unavailable")
            return sourceId
        }
    }

    private class RecordingIndexer(
        private val outcome: SafPdfFolderIndexingOutcome = SafPdfFolderIndexingOutcome.Indexed(
            sourceId = SourceId("android-saf-document-tree:test"),
            discoveredAssetCount = 0,
            hasMore = false,
        ),
    ) : SafPdfFolderIndexer {
        val receivedSourceIds = mutableListOf<SourceId>()

        override suspend fun invoke(sourceId: SourceId): SafPdfFolderIndexingOutcome {
            receivedSourceIds += sourceId
            return outcome
        }
    }
}
