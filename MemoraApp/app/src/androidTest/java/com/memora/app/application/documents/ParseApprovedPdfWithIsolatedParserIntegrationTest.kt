package com.memora.app.application.documents

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.pdfbox.isolation.AndroidIsolatedPdfParserConnection
import com.memora.app.data.pdfbox.isolation.BorrowedPdfDescriptorParser
import com.memora.app.data.pdfbox.isolation.ContextIsolatedPdfParserServiceBinder
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClient
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClientOutcome
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClientResult
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserDescriptorHandoff
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserBindingStatus
import com.memora.app.data.pdfbox.isolation.toApprovedPdfParsingOutcome
import com.memora.app.data.saf.SafPdfReadOnlyDescriptorAccess
import com.memora.app.data.saf.SyntheticPdfDocumentsProvider
import com.memora.app.data.saf.SafPdfDocumentFingerprint
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.discovery.SourceAccessState
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises one repository-owned fixture through the approved-custody and isolated-parser
 * boundaries. It has no real SAF grant, user document, Room write, UI, worker, AI, or network.
 */
@RunWith(AndroidJUnit4::class)
class ParseApprovedPdfWithIsolatedParserIntegrationTest {
    private val sourceId = SourceId("android-saf-document-tree:synthetic")
    private val approval = DocumentTreeApproval(
        sourceId = sourceId,
        treeUri = "content://com.memora.app.debug.syntheticpdfdocuments/tree/memora-root",
        approvedAt = Instant.parse("2026-07-23T00:00:00Z"),
    )

    private lateinit var connection: AndroidIsolatedPdfParserConnection
    private lateinit var client: IsolatedPdfParserClient

    @Before
    fun connectPrivateParser() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        connection = AndroidIsolatedPdfParserConnection(ContextIsolatedPdfParserServiceBinder(context))
        client = IsolatedPdfParserClient(connection)

        assertEquals(IsolatedPdfParserBindingStatus.CONNECTING, connection.connect())
        assertEquals(IsolatedPdfParserBindingStatus.AVAILABLE, connection.awaitAvailability(10_000))
    }

    @After
    fun closePrivateParser() {
        client.close()
        connection.close()
    }

    @Test
    fun parses_only_the_debug_fixture_through_approved_custody_and_the_private_service() = runBlocking {
        val coordinator = coordinator(
            accessStates = listOf(SourceAccessState.GRANTED, SourceAccessState.GRANTED),
            parser = IsolatedPdfParserDescriptorHandoff(client),
        )

        val outcome = coordinator.execute(request())

        assertEquals(ApprovedPdfParsingOutcome.Extracted(pageCount = 1), outcome)
    }

    @Test
    fun revoked_access_before_open_never_submits_a_descriptor_to_the_parser() = runBlocking {
        val parserCalls = AtomicInteger(0)
        val coordinator = coordinator(
            accessStates = listOf(SourceAccessState.GRANTED, SourceAccessState.ACCESS_REVOKED),
            parser = BorrowedPdfDescriptorParser { _, _ ->
                parserCalls.incrementAndGet()
                error("A revoked grant must stop before parser submission.")
            },
        )

        val outcome = coordinator.execute(request())

        assertEquals(ApprovedPdfParsingOutcome.AccessRevoked, outcome)
        assertEquals(0, parserCalls.get())
    }

    @Test
    fun content_free_retryable_parser_failure_is_not_represented_as_extraction() = runBlocking {
        val parserCalls = AtomicInteger(0)
        val coordinator = coordinator(
            accessStates = listOf(SourceAccessState.GRANTED, SourceAccessState.GRANTED),
            parser = BorrowedPdfDescriptorParser { _, _ ->
                parserCalls.incrementAndGet()
                IsolatedPdfParserClientResult(
                    outcome = IsolatedPdfParserClientOutcome.FAILURE,
                    retryable = true,
                )
            },
        )

        val outcome = coordinator.execute(request())

        assertEquals(ApprovedPdfParsingOutcome.RetryableFailure, outcome)
        assertEquals(1, parserCalls.get())
    }

    private fun coordinator(
        accessStates: List<SourceAccessState>,
        parser: BorrowedPdfDescriptorParser,
    ): ParseApprovedPdfWithIsolatedParser = ParseApprovedPdfWithIsolatedParser(
        descriptorAccess = SafPdfReadOnlyDescriptorAccess(
            approvalRepository = FakeApprovalRepository(approval),
            accessValidator = SequencedAccessValidator(accessStates),
            context = ApplicationProvider.getApplicationContext(),
        ),
        parser = ApprovedPdfBorrowedParser { descriptor, signal ->
            parser.parseBorrowed(descriptor, signal).toApprovedPdfParsingOutcome()
        },
    )

    private fun request(): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = sourceId,
                sourceAssetKey = SourceAssetKey(SyntheticPdfDocumentsProvider.PDF_DOCUMENT_ID),
            ),
            type = AssetType.PDF,
            location = AssetLocation(
                "content://com.memora.app.debug.syntheticpdfdocuments/document/ignored-location",
            ),
            fingerprint = SafPdfDocumentFingerprint.from(
                documentId = SyntheticPdfDocumentsProvider.PDF_DOCUMENT_ID,
                lastModifiedEpochMillis = SyntheticPdfDocumentsProvider.FIXTURE_MODIFIED_AT,
                sizeBytes = SyntheticPdfDocumentsProvider.SYNTHETIC_PDF.size.toLong(),
                mimeType = SyntheticPdfDocumentsProvider.PDF_MIME_TYPE,
            ),
            discoveredAt = Instant.parse("2026-07-23T00:00:00Z"),
        ),
        schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
    )

    private class FakeApprovalRepository(
        private val approval: DocumentTreeApproval,
    ) : DocumentTreeApprovalRepository {
        override suspend fun save(approval: DocumentTreeApproval) = Unit

        override suspend fun findAll(): List<DocumentTreeApproval> = listOf(approval)
    }

    private class SequencedAccessValidator(
        states: List<SourceAccessState>,
    ) : DocumentTreeAccessValidator {
        private val states = ArrayDeque(states)

        override suspend fun accessState(approval: DocumentTreeApproval): SourceAccessState =
            if (states.isEmpty()) SourceAccessState.ACCESS_REVOKED else states.removeFirst()
    }
}
