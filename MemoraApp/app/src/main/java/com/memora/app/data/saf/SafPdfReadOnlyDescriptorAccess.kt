package com.memora.app.data.saf

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.memora.app.application.documents.PdfReadOnlyDescriptorAccess
import com.memora.app.application.documents.PdfReadOnlyDescriptorOutcome
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.extraction.PdfExtractionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafPdfReadOnlyDescriptorAccess @Inject constructor(
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val accessValidator: DocumentTreeAccessValidator,
    @param:ApplicationContext context: Context,
) : PdfReadOnlyDescriptorAccess {
    private val platform: SafPdfDescriptorPlatform = ContentResolverSafPdfDescriptorPlatform(context)
    private val broker = SafPdfDescriptorBroker(
        approvalRepository = approvalRepository,
        accessValidator = accessValidator,
        platform = platform,
    )

    override suspend fun <T> withReadOnlyDescriptor(
        request: PdfExtractionRequest,
        cancellationSignal: CancellationSignal?,
        consume: (ParcelFileDescriptor) -> T,
    ): PdfReadOnlyDescriptorOutcome<T> = when (
        val brokerResult = broker.withReadOnlyDescriptor(request, cancellationSignal, consume)
    ) {
        is SafPdfDescriptorBrokerResult.Consumed ->
            PdfReadOnlyDescriptorOutcome.Consumed(brokerResult.value)
        SafPdfDescriptorBrokerResult.AccessRequired ->
            PdfReadOnlyDescriptorOutcome.AccessRequired
        SafPdfDescriptorBrokerResult.AccessRevoked ->
            PdfReadOnlyDescriptorOutcome.AccessRevoked
        SafPdfDescriptorBrokerResult.SourceUnavailable ->
            PdfReadOnlyDescriptorOutcome.SourceUnavailable
        SafPdfDescriptorBrokerResult.SourceMismatch ->
            PdfReadOnlyDescriptorOutcome.SourceMismatch
        SafPdfDescriptorBrokerResult.StaleSource ->
            PdfReadOnlyDescriptorOutcome.StaleSource
        SafPdfDescriptorBrokerResult.InvalidTarget ->
            PdfReadOnlyDescriptorOutcome.InvalidTarget
        SafPdfDescriptorBrokerResult.TreeMembershipDenied ->
            PdfReadOnlyDescriptorOutcome.TreeMembershipDenied
        SafPdfDescriptorBrokerResult.UnsupportedPlatform ->
            PdfReadOnlyDescriptorOutcome.UnsupportedPlatform
        SafPdfDescriptorBrokerResult.Cancelled ->
            PdfReadOnlyDescriptorOutcome.Cancelled
        SafPdfDescriptorBrokerResult.RetryableFailure ->
            PdfReadOnlyDescriptorOutcome.RetryableFailure
    }
}
