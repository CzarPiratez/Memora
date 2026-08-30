package com.memora.app.application.documents

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.memora.app.domain.extraction.PdfExtractionRequest

/**
 * Application port for one read-only SAF PDF descriptor custody attempt.
 *
 * Implementations own platform open/close semantics. Callers supply a consumer that
 * runs while the duplicate descriptor remains open.
 */
interface PdfReadOnlyDescriptorAccess {
    suspend fun <T> withReadOnlyDescriptor(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal? = null,
        consume: (ParcelFileDescriptor) -> T,
    ): PdfReadOnlyDescriptorOutcome<T>
}

/** Content-free custody outcome for an approved PDF descriptor handoff. */
sealed interface PdfReadOnlyDescriptorOutcome<out T> {
    data class Consumed<T>(val value: T) : PdfReadOnlyDescriptorOutcome<T>

    data object AccessRequired : PdfReadOnlyDescriptorOutcome<Nothing>

    data object AccessRevoked : PdfReadOnlyDescriptorOutcome<Nothing>

    data object SourceUnavailable : PdfReadOnlyDescriptorOutcome<Nothing>

    data object SourceMismatch : PdfReadOnlyDescriptorOutcome<Nothing>

    data object StaleSource : PdfReadOnlyDescriptorOutcome<Nothing>

    data object InvalidTarget : PdfReadOnlyDescriptorOutcome<Nothing>

    data object TreeMembershipDenied : PdfReadOnlyDescriptorOutcome<Nothing>

    data object UnsupportedPlatform : PdfReadOnlyDescriptorOutcome<Nothing>

    data object Cancelled : PdfReadOnlyDescriptorOutcome<Nothing>

    data object RetryableFailure : PdfReadOnlyDescriptorOutcome<Nothing>
}
