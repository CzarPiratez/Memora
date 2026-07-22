package com.memora.app.data.saf

import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.os.ParcelFileDescriptor
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.discovery.SourceAccessState
import com.memora.app.domain.extraction.ApprovedPdfDescriptorCustodyContract
import com.memora.app.domain.extraction.PdfDescriptorCustodyDecision
import com.memora.app.domain.extraction.PdfExtractionRequest
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ordinary-process custody boundary for one future SAF PDF parser request.
 *
 * The broker contains all descriptor ownership: it opens one read-only descriptor,
 * duplicates it once, closes the original before the supplied data-layer consumer
 * executes, then closes the duplicate on every exit. It is intentionally internal,
 * unbound from Hilt, and has no UI, Room persistence, worker, or parser-service
 * dependency. The only current caller is its synthetic Android integration test.
 */
internal class SafPdfDescriptorBroker(
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val accessValidator: DocumentTreeAccessValidator,
    private val platform: SafPdfDescriptorPlatform,
) {
    suspend fun <T> withReadOnlyDescriptor(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal? = null,
        consume: (ParcelFileDescriptor) -> T,
    ): SafPdfDescriptorBrokerResult<T> = withContext(Dispatchers.IO) {
        if (cancellationSignal?.isCanceled == true) {
            return@withContext SafPdfDescriptorBrokerResult.Cancelled
        }
        val approval = approvalRepository.find(request.asset.identity.sourceId)
            ?: return@withContext SafPdfDescriptorBrokerResult.AccessRequired

        val initialAccess = accessValidator.accessState(approval)
        if (initialAccess != SourceAccessState.GRANTED) {
            return@withContext initialAccess.toBrokerResult()
        }
        when (
            ApprovedPdfDescriptorCustodyContract.decide(
                request = request,
                approval = approval,
                accessState = initialAccess,
            )
        ) {
            is PdfDescriptorCustodyDecision.Authorized -> Unit
            PdfDescriptorCustodyDecision.AccessRequired -> {
                return@withContext SafPdfDescriptorBrokerResult.AccessRequired
            }
            PdfDescriptorCustodyDecision.AccessRevoked -> {
                return@withContext SafPdfDescriptorBrokerResult.AccessRevoked
            }
            PdfDescriptorCustodyDecision.SourceUnavailable -> {
                return@withContext SafPdfDescriptorBrokerResult.SourceUnavailable
            }
            PdfDescriptorCustodyDecision.SourceMismatch -> {
                return@withContext SafPdfDescriptorBrokerResult.SourceMismatch
            }
        }

        val target = when (val result = SafPdfCanonicalDocumentTargetFactory.create(approval, request.asset)) {
            is SafPdfCanonicalTargetResult.Target -> result.value
            SafPdfCanonicalTargetResult.NotPdf -> {
                return@withContext SafPdfDescriptorBrokerResult.InvalidTarget
            }
            SafPdfCanonicalTargetResult.SourceMismatch -> {
                return@withContext SafPdfDescriptorBrokerResult.SourceMismatch
            }
            SafPdfCanonicalTargetResult.InvalidApprovedTree,
            SafPdfCanonicalTargetResult.ForeignAssetLocation,
            -> return@withContext SafPdfDescriptorBrokerResult.InvalidTarget
        }

        when (platform.membership(target)) {
            SafPdfTreeMembership.VERIFIED -> Unit
            SafPdfTreeMembership.REJECTED -> {
                return@withContext SafPdfDescriptorBrokerResult.TreeMembershipDenied
            }
            SafPdfTreeMembership.UNSUPPORTED -> {
                return@withContext SafPdfDescriptorBrokerResult.UnsupportedPlatform
            }
            SafPdfTreeMembership.UNAVAILABLE -> {
                return@withContext SafPdfDescriptorBrokerResult.SourceUnavailable
            }
        }

        if (cancellationSignal?.isCanceled == true) {
            return@withContext SafPdfDescriptorBrokerResult.Cancelled
        }
        val finalAccess = accessValidator.accessState(approval)
        if (finalAccess != SourceAccessState.GRANTED) {
            return@withContext finalAccess.toBrokerResult()
        }

        val duplicate = try {
            val opened = platform.openReadOnly(target, cancellationSignal)
                ?: return@withContext SafPdfDescriptorBrokerResult.SourceUnavailable
            opened.use { it.dup() }
        } catch (_: OperationCanceledException) {
            return@withContext SafPdfDescriptorBrokerResult.Cancelled
        } catch (_: SecurityException) {
            return@withContext SafPdfDescriptorBrokerResult.AccessRevoked
        } catch (_: FileNotFoundException) {
            return@withContext SafPdfDescriptorBrokerResult.SourceUnavailable
        } catch (_: IOException) {
            return@withContext SafPdfDescriptorBrokerResult.RetryableFailure
        } catch (_: IllegalArgumentException) {
            return@withContext SafPdfDescriptorBrokerResult.InvalidTarget
        }

        duplicate.use {
            if (cancellationSignal?.isCanceled == true) {
                return@withContext SafPdfDescriptorBrokerResult.Cancelled
            }
            return@withContext SafPdfDescriptorBrokerResult.Consumed(consume(it))
        }
    }

    private fun SourceAccessState.toBrokerResult(): SafPdfDescriptorBrokerResult<Nothing> = when (this) {
        SourceAccessState.GRANTED -> error("Granted access must not be converted to a denial.")
        SourceAccessState.ACCESS_REQUIRED -> SafPdfDescriptorBrokerResult.AccessRequired
        SourceAccessState.ACCESS_REVOKED -> SafPdfDescriptorBrokerResult.AccessRevoked
        SourceAccessState.UNAVAILABLE -> SafPdfDescriptorBrokerResult.SourceUnavailable
    }
}

/** Android platform operations required after the pure source-custody decision. */
internal interface SafPdfDescriptorPlatform {
    fun membership(target: SafPdfCanonicalTarget): SafPdfTreeMembership

    @Throws(
        OperationCanceledException::class,
        SecurityException::class,
        FileNotFoundException::class,
        IOException::class,
    )
    fun openReadOnly(
        target: SafPdfCanonicalTarget,
        cancellationSignal: CancellationSignal?,
    ): ParcelFileDescriptor?
}

/** The platform reports no source details to the broker on a membership denial. */
internal enum class SafPdfTreeMembership {
    VERIFIED,
    REJECTED,
    UNSUPPORTED,
    UNAVAILABLE,
}

/** Content-free result of a descriptor custody attempt. */
internal sealed interface SafPdfDescriptorBrokerResult<out T> {
    data class Consumed<T>(val value: T) : SafPdfDescriptorBrokerResult<T>

    data object AccessRequired : SafPdfDescriptorBrokerResult<Nothing>

    data object AccessRevoked : SafPdfDescriptorBrokerResult<Nothing>

    data object SourceUnavailable : SafPdfDescriptorBrokerResult<Nothing>

    data object SourceMismatch : SafPdfDescriptorBrokerResult<Nothing>

    data object InvalidTarget : SafPdfDescriptorBrokerResult<Nothing>

    data object TreeMembershipDenied : SafPdfDescriptorBrokerResult<Nothing>

    data object UnsupportedPlatform : SafPdfDescriptorBrokerResult<Nothing>

    data object Cancelled : SafPdfDescriptorBrokerResult<Nothing>

    data object RetryableFailure : SafPdfDescriptorBrokerResult<Nothing>
}
