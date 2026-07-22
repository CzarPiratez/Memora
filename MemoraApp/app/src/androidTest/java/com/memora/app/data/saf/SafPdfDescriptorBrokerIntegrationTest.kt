package com.memora.app.data.saf

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
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
import java.io.FileDescriptor
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises descriptor custody against only the debug APK's synthetic provider.
 * It neither asks Android for a real grant nor opens a user source. There is no parser,
 * Room persistence, UI, worker, AI, or network call. The fixture is excluded from release.
 */
@RunWith(AndroidJUnit4::class)
class SafPdfDescriptorBrokerIntegrationTest {
    private val sourceId = SourceId("android-saf-document-tree:synthetic")
    private val approval = DocumentTreeApproval(
        sourceId = sourceId,
        treeUri = "content://com.memora.app.debug.syntheticpdfdocuments/tree/memora-root",
        approvedAt = Instant.parse("2026-07-23T00:00:00Z"),
    )

    @Test
    fun opens_only_one_read_only_synthetic_descriptor_then_closes_the_duplicate() = runBlocking {
        val platform = ContentResolverSafPdfDescriptorPlatform(ApplicationProvider.getApplicationContext())
        val broker = broker(
            accessStates = listOf(SourceAccessState.GRANTED, SourceAccessState.GRANTED),
            platform = platform,
        )
        var descriptorWasValidInsideConsumer = false

        val result = broker.withReadOnlyDescriptor(request()) { descriptor ->
            descriptorWasValidInsideConsumer = descriptor.fileDescriptor.valid()
            "consumed"
        }

        assertEquals(SafPdfDescriptorBrokerResult.Consumed("consumed"), result)
        assertTrue(descriptorWasValidInsideConsumer)
    }

    @Test
    fun closes_original_before_consumer_then_closes_duplicate_after_consumer_returns() = runBlocking {
        val pipe = ParcelFileDescriptor.createPipe()
        pipe[1].close()
        val platform = ClosingObservationPlatform(pipe[0])
        var duplicateDescriptor: FileDescriptor? = null

        val result = broker(
            accessStates = listOf(SourceAccessState.GRANTED, SourceAccessState.GRANTED),
            platform = platform,
        ).withReadOnlyDescriptor(request()) { descriptor: ParcelFileDescriptor ->
            duplicateDescriptor = descriptor.fileDescriptor
            assertFalse(platform.originalDescriptor.valid())
            assertTrue(duplicateDescriptor?.valid() == true)
            "consumed"
        }

        assertEquals(SafPdfDescriptorBrokerResult.Consumed("consumed"), result)
        assertFalse(platform.originalDescriptor.valid())
        assertFalse(duplicateDescriptor?.valid() == true)
    }

    @Test
    fun revoked_before_target_creation_does_not_call_the_platform() = runBlocking {
        val platform = RecordingPlatform()
        val result = broker(
            accessStates = listOf(SourceAccessState.ACCESS_REVOKED),
            platform = platform,
        ).withReadOnlyDescriptor(request()) { "must not run" }

        assertEquals(SafPdfDescriptorBrokerResult.AccessRevoked, result)
        assertEquals(0, platform.membershipCalls.get())
        assertEquals(0, platform.openCalls.get())
    }

    @Test
    fun revoked_immediately_before_open_does_not_open_the_provider() = runBlocking {
        val platform = RecordingPlatform()
        val result = broker(
            accessStates = listOf(SourceAccessState.GRANTED, SourceAccessState.ACCESS_REVOKED),
            platform = platform,
        ).withReadOnlyDescriptor(request()) { "must not run" }

        assertEquals(SafPdfDescriptorBrokerResult.AccessRevoked, result)
        assertEquals(1, platform.membershipCalls.get())
        assertEquals(0, platform.openCalls.get())
    }

    @Test
    fun rejected_tree_membership_does_not_open_a_descriptor() = runBlocking {
        val platform = RecordingPlatform(membership = SafPdfTreeMembership.REJECTED)
        val result = broker(
            accessStates = listOf(SourceAccessState.GRANTED),
            platform = platform,
        ).withReadOnlyDescriptor(request()) { "must not run" }

        assertEquals(SafPdfDescriptorBrokerResult.TreeMembershipDenied, result)
        assertEquals(1, platform.membershipCalls.get())
        assertEquals(0, platform.openCalls.get())
    }

    @Test
    fun foreign_source_without_an_approval_is_denied_before_platform_access() = runBlocking {
        val platform = RecordingPlatform()
        val result = broker(
            accessStates = listOf(SourceAccessState.GRANTED),
            platform = platform,
        ).withReadOnlyDescriptor(request(sourceId = SourceId("android-saf-document-tree:foreign"))) {
            "must not run"
        }

        assertEquals(SafPdfDescriptorBrokerResult.AccessRequired, result)
        assertEquals(0, platform.membershipCalls.get())
        assertEquals(0, platform.openCalls.get())
    }

    private fun broker(
        accessStates: List<SourceAccessState>,
        platform: SafPdfDescriptorPlatform,
    ): SafPdfDescriptorBroker = SafPdfDescriptorBroker(
        approvalRepository = FakeApprovalRepository(approval),
        accessValidator = SequencedAccessValidator(accessStates),
        platform = platform,
    )

    private fun request(sourceId: SourceId = this.sourceId): PdfExtractionRequest = PdfExtractionRequest(
        asset = Asset(
            identity = AssetIdentity(
                sourceId = sourceId,
                sourceAssetKey = SourceAssetKey(SyntheticPdfDocumentsProvider.PDF_DOCUMENT_ID),
            ),
            type = AssetType.PDF,
            location = AssetLocation(
                "content://com.memora.app.debug.syntheticpdfdocuments/document/ignored-location",
            ),
            fingerprint = AssetFingerprint("synthetic-report:42:1024:application/pdf"),
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
            if (states.isEmpty()) {
                SourceAccessState.ACCESS_REVOKED
            } else {
                states.removeFirst()
            }
    }

    private class RecordingPlatform(
        private val membership: SafPdfTreeMembership = SafPdfTreeMembership.VERIFIED,
    ) : SafPdfDescriptorPlatform {
        val membershipCalls = AtomicInteger(0)
        val openCalls = AtomicInteger(0)

        override fun membership(target: SafPdfCanonicalTarget): SafPdfTreeMembership {
            membershipCalls.incrementAndGet()
            return membership
        }

        override fun openReadOnly(
            target: SafPdfCanonicalTarget,
            cancellationSignal: CancellationSignal?,
        ): ParcelFileDescriptor? {
            openCalls.incrementAndGet()
            throw AssertionError("The recording platform must not open a descriptor in this test.")
        }
    }

    private class ClosingObservationPlatform(
        private val original: ParcelFileDescriptor,
    ) : SafPdfDescriptorPlatform {
        val originalDescriptor: FileDescriptor = original.fileDescriptor

        override fun membership(target: SafPdfCanonicalTarget): SafPdfTreeMembership =
            SafPdfTreeMembership.VERIFIED

        override fun openReadOnly(
            target: SafPdfCanonicalTarget,
            cancellationSignal: CancellationSignal?,
        ): ParcelFileDescriptor = original
    }
}
