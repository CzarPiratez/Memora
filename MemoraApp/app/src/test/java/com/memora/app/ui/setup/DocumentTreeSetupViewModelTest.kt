package com.memora.app.ui.setup

import androidx.work.Data
import androidx.work.WorkInfo
import com.memora.app.application.documents.DocumentTreeApprover
import com.memora.app.application.documents.PdfFolderConnectionFinder
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.DocumentTreeSource
import com.memora.app.work.SafPdfDiscoveryWorkScheduler
import com.memora.app.work.SafPdfDiscoveryWorker
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
        val viewModel = DocumentTreeSetupViewModel(
            approver,
            finder,
            RecordingScheduler(),
            RecordingAssetRepository(),
        )

        assertEquals(DocumentTreeConnectionState.LOADING, viewModel.uiState.value.connection)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(DocumentTreeConnectionState.READY, viewModel.uiState.value.connection)
        assertEquals(0, approver.invocationCount)
        assertEquals(1, finder.invocationCount)
    }

    @Test
    fun restoresTheMostRecentSavedFolderForAnExplicitIndexingRequest() = runTest {
        val sourceId = SourceId("android-saf-document-tree:restored")
        val scheduler = RecordingScheduler()
        val assets = RecordingAssetRepository(pdfCount = 1)
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(sourceId = sourceId),
            scheduler,
            assets,
        )

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(DocumentTreeConnectionState.CONNECTED(sourceId), viewModel.uiState.value.connection)

        viewModel.onIndexRequested()
        assertEquals(listOf(sourceId), scheduler.drainRequests)
        assertEquals(PdfFolderIndexingState.IN_PROGRESS, viewModel.uiState.value.indexing)

        scheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.SUCCEEDED,
                    output = Data.Builder()
                        .putBoolean(SafPdfDiscoveryWorker.KEY_HAS_MORE, false)
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            PdfFolderIndexingState.COMPLETED(discoveredAssetCount = 1, hasMore = false),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun savesOnlyTheUriReportedAfterAndroidPersistsReadAccess() = runTest {
        val approver = RecordingApprover()
        val viewModel = DocumentTreeSetupViewModel(
            approver,
            RecordingFinder(),
            RecordingScheduler(),
            RecordingAssetRepository(),
        )
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
        assertEquals(PdfFolderIndexingState.NOT_STARTED, viewModel.uiState.value.indexing)
    }

    @Test
    fun ignoresStaleFinishedDiscoveryWorkUntilUserStartsIndexing() = runTest {
        val treeUri = "content://example/tree/documents"
        val sourceId = DocumentTreeSource.sourceIdFor(treeUri)
        val scheduler = RecordingScheduler()
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(),
            scheduler,
            RecordingAssetRepository(pdfCount = 0),
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPersistedReadAccessReceived(treeUri)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(PdfFolderIndexingState.NOT_STARTED, viewModel.uiState.value.indexing)

        // Prior session finished unique work still reported by WorkManager after clear.
        scheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.SUCCEEDED,
                    output = Data.Builder()
                        .putBoolean(SafPdfDiscoveryWorker.KEY_HAS_MORE, false)
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(PdfFolderIndexingState.NOT_STARTED, viewModel.uiState.value.indexing)
        assertEquals(DocumentTreeConnectionState.CONNECTED(sourceId), viewModel.uiState.value.connection)
    }

    @Test
    fun reportsAPersistableGrantFailureWithoutSavingAnything() = runTest {
        val approver = RecordingApprover()
        val viewModel = DocumentTreeSetupViewModel(
            approver,
            RecordingFinder(),
            RecordingScheduler(),
            RecordingAssetRepository(),
        )

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
            RecordingScheduler(),
            RecordingAssetRepository(),
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
        val scheduler = RecordingScheduler()
        val assets = RecordingAssetRepository(pdfCount = 2)
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(),
            scheduler,
            assets,
        )

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIndexRequested()
        assertEquals(emptyList<SourceId>(), scheduler.drainRequests)

        viewModel.onPersistedReadAccessReceived(treeUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIndexRequested()
        assertEquals(PdfFolderIndexingState.IN_PROGRESS, viewModel.uiState.value.indexing)
        assertEquals(listOf(sourceId), scheduler.drainRequests)

        scheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.SUCCEEDED,
                    output = Data.Builder()
                        .putBoolean(SafPdfDiscoveryWorker.KEY_HAS_MORE, false)
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            PdfFolderIndexingState.COMPLETED(discoveredAssetCount = 2, hasMore = false),
            viewModel.uiState.value.indexing,
        )
    }

    @Test
    fun reportsRevokedFolderAccessAsAConnectionRecoveryState() = runTest {
        val treeUri = "content://example/tree/documents"
        val scheduler = RecordingScheduler()
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(),
            scheduler,
            RecordingAssetRepository(),
        )

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPersistedReadAccessReceived(treeUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIndexRequested()
        assertEquals(PdfFolderIndexingState.IN_PROGRESS, viewModel.uiState.value.indexing)

        scheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.FAILED,
                    output = Data.Builder()
                        .putString(
                            SafPdfDiscoveryWorker.KEY_FAILURE_REASON,
                            SafPdfDiscoveryWorker.REASON_ACCESS_STOPPED,
                        )
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()

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
        val sourceId = DocumentTreeSource.sourceIdFor(treeUri)
        val scheduler = RecordingScheduler()
        val viewModel = DocumentTreeSetupViewModel(
            RecordingApprover(),
            RecordingFinder(),
            scheduler,
            RecordingAssetRepository(),
        )

        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPersistedReadAccessReceived(treeUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIndexRequested()

        scheduler.emit(
            listOf(
                workInfo(
                    state = WorkInfo.State.FAILED,
                    output = Data.Builder()
                        .putString(SafPdfDiscoveryWorker.KEY_FAILURE_REASON, "retryable")
                        .build(),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            PdfFolderIndexingState.FAILED(
                "Memora could not finish reading PDF folder metadata. You can try again.",
            ),
            viewModel.uiState.value.indexing,
        )
        assertEquals(
            DocumentTreeConnectionState.CONNECTED(sourceId),
            viewModel.uiState.value.connection,
        )
    }

    private fun workInfo(state: WorkInfo.State, output: Data): WorkInfo =
        WorkInfo(
            UUID.randomUUID(),
            state,
            emptySet(),
            output,
            Data.EMPTY,
            1,
            1,
        )

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

    private class RecordingScheduler : SafPdfDiscoveryWorkScheduler {
        val drainRequests = mutableListOf<SourceId>()
        private val infos = MutableStateFlow<List<WorkInfo>>(emptyList())

        override fun enqueueDrain(sourceId: SourceId) {
            drainRequests += sourceId
        }

        override fun enqueueContinuation(sourceId: SourceId) = Unit

        override fun observeUniqueWork(sourceId: SourceId): Flow<List<WorkInfo>> = infos

        fun emit(value: List<WorkInfo>) {
            infos.value = value
        }
    }

    private class RecordingAssetRepository(
        private val pdfCount: Int = 0,
    ) : AssetRepository {
        override suspend fun save(record: AssetIndexRecord) = Unit

        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = null

        override suspend fun findFirstBySourceAndType(
            sourceId: SourceId,
            type: AssetType,
        ): Asset? = null

        override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int =
            if (type == AssetType.PDF) pdfCount else 0

        override suspend fun findNextPdfPendingLocalReading(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null

        override suspend fun findNextImagePendingExifExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
    }
}
