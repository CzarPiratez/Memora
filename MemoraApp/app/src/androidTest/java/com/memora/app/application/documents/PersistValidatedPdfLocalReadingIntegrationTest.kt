package com.memora.app.application.documents

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.pdfbox.SyntheticPdfFixtures
import com.memora.app.data.pdfbox.isolation.AndroidIsolatedPdfParserConnection
import com.memora.app.data.pdfbox.isolation.ContextIsolatedPdfParserServiceBinder
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserBindingStatus
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClient
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClientOutcome
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserDescriptorHandoff
import com.memora.app.data.saf.ContentResolverDocumentTreeAccessValidator
import com.memora.app.data.saf.ContentResolverSafPdfDescriptorPlatform
import com.memora.app.data.saf.SafPdfDescriptorBroker
import com.memora.app.data.saf.SafPdfDescriptorBrokerResult
import com.memora.app.data.saf.SafPdfDocumentFingerprint
import com.memora.app.data.security.MemoraEncryptedDatabaseOpener
import com.memora.app.data.local.RoomDocumentTreeApprovalRepository
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens one fixture PDF under the user-approved SAF tree, retains validated wire text in the
 * ordinary process, and persists searchable page text to encrypted Room. No WorkManager, AI,
 * or network.
 *
 * Requires: Connect a PDF folder in Memora before running.
 */
@RunWith(AndroidJUnit4::class)
class PersistValidatedPdfLocalReadingIntegrationTest {
    private lateinit var context: Context
    private lateinit var applicationDatabase: MemoraDatabase
    private lateinit var connection: AndroidIsolatedPdfParserConnection
    private lateinit var client: IsolatedPdfParserClient
    private var createdDocumentUri: Uri? = null
    private var persistedKey: PersistedKey? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        applicationDatabase = MemoraEncryptedDatabaseOpener.open(context)
        connection = AndroidIsolatedPdfParserConnection(ContextIsolatedPdfParserServiceBinder(context))
        client = IsolatedPdfParserClient(connection)
        assertTrue(connection.connect() == IsolatedPdfParserBindingStatus.CONNECTING)
        assertTrue(connection.awaitAvailability(10_000) == IsolatedPdfParserBindingStatus.AVAILABLE)
    }

    @After
    fun tearDown() {
        persistedKey?.let { key ->
            runBlocking {
                applicationDatabase.pdfExtractionDao().deleteHeader(
                    sourceId = key.sourceId,
                    sourceAssetKey = key.sourceAssetKey,
                    fingerprint = key.fingerprint,
                    schemaVersion = key.schemaVersion,
                )
            }
        }
        createdDocumentUri?.let { uri ->
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
        client.close()
        connection.close()
        applicationDatabase.close()
    }

    @Test
    fun persists_searchable_page_text_from_approved_tree_fixture() = runBlocking {
        val approvalRepository = RoomDocumentTreeApprovalRepository {
            applicationDatabase.documentTreeApprovalDao()
        }
        val approval = requireNotNull(approvalRepository.findAll().firstOrNull()) {
            "Connect a PDF folder in Memora before running this emulator integration test."
        }

        val fixture = createFixturePdfInApprovedTree(approval)
        createdDocumentUri = fixture.documentUri

        val request = PdfExtractionRequest(
            asset = Asset(
                identity = AssetIdentity(
                    sourceId = approval.sourceId,
                    sourceAssetKey = SourceAssetKey(fixture.documentId),
                ),
                type = AssetType.PDF,
                location = AssetLocation(fixture.documentUri.toString()),
                fingerprint = fixture.fingerprint,
                discoveredAt = Instant.now(),
            ),
            schemaVersion = ExtractionSchemaVersion("pdf-extraction-v1"),
        )

        val broker = SafPdfDescriptorBroker(
            approvalRepository = approvalRepository,
            accessValidator = ContentResolverDocumentTreeAccessValidator(context),
            platform = ContentResolverSafPdfDescriptorPlatform(context),
        )
        val handoff = IsolatedPdfParserDescriptorHandoff(client)
        val brokerResult = broker.withReadOnlyDescriptor(request) {
            handoff.parseBorrowed(it, cancellationSignal = null)
        }
        require(brokerResult is SafPdfDescriptorBrokerResult.Consumed) {
            "Expected broker to open the fixture PDF, was $brokerResult"
        }
        val clientResult = brokerResult.value

        assertEquals(IsolatedPdfParserClientOutcome.EXTRACTED, clientResult.outcome)
        assertNotNull(clientResult.validatedResult)

        val status = PersistValidatedPdfLocalReading(applicationDatabase)
            .execute(request, clientResult)
        assertEquals(PdfLocalReadingStatusCheckResult.Completed, status)

        val sourceId = request.asset.identity.sourceId.value
        val sourceAssetKey = request.asset.identity.sourceAssetKey.value
        val fingerprint = request.asset.fingerprint.value
        val schemaVersion = request.schemaVersion.value
        persistedKey = PersistedKey(sourceId, sourceAssetKey, fingerprint, schemaVersion)

        val header = applicationDatabase.pdfExtractionDao().findHeader(
            sourceId = sourceId,
            sourceAssetKey = sourceAssetKey,
            fingerprint = fingerprint,
            schemaVersion = schemaVersion,
        )
        assertNotNull(header)
        assertTrue(requireNotNull(header).pageCount > 0)

        val pages = applicationDatabase.pdfExtractionDao().findPages(
            sourceId = sourceId,
            sourceAssetKey = sourceAssetKey,
            fingerprint = fingerprint,
            schemaVersion = schemaVersion,
        )
        assertTrue(pages.isNotEmpty())
        assertTrue(pages.any { page -> page.pageText.isNotBlank() })
    }

    private fun createFixturePdfInApprovedTree(approval: DocumentTreeApproval): CreatedFixture {
        val treeUri = Uri.parse(approval.treeUri)
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val resolver: ContentResolver = context.contentResolver
        val documentUri = requireNotNull(
            DocumentsContract.createDocument(
                resolver,
                parentDocumentUri,
                "application/pdf",
                "memora-persist-fixture.pdf",
            ),
        ) {
            "Android did not create a fixture PDF under the approved tree."
        }
        val bytes = SyntheticPdfFixtures.twoPageSelectable().readBytes()
        resolver.openOutputStream(documentUri)?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: fail("Could not write the fixture PDF under the approved tree.")

        val documentId = DocumentsContract.getDocumentId(documentUri)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val fingerprint = requireNotNull(
            resolver.query(documentUri, projection, null, null, null)?.use { cursor ->
                require(cursor.moveToFirst())
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                SafPdfDocumentFingerprint.from(
                    documentId = documentId,
                    lastModifiedEpochMillis = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                        cursor.getLong(modifiedIndex)
                    } else {
                        null
                    },
                    sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        null
                    },
                    mimeType = cursor.getString(mimeIndex) ?: "application/pdf",
                )
            },
        ) {
            "Could not observe fingerprint metadata for the fixture PDF."
        }

        return CreatedFixture(
            documentUri = documentUri,
            documentId = documentId,
            fingerprint = fingerprint,
        )
    }

    private data class CreatedFixture(
        val documentUri: Uri,
        val documentId: String,
        val fingerprint: com.memora.app.domain.asset.AssetFingerprint,
    )

    private data class PersistedKey(
        val sourceId: String,
        val sourceAssetKey: String,
        val fingerprint: String,
        val schemaVersion: String,
    )
}
