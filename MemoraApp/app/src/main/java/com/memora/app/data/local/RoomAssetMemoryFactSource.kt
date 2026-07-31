package com.memora.app.data.local

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.ImageExifSchemaVersion
import com.memora.app.domain.extraction.PhotoOcrSchemaVersion
import com.memora.app.domain.extraction.ScreenshotOcrSchemaVersion
import com.memora.app.domain.memory.AssetMemoryFact
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEvidenceKind

class RoomAssetMemoryFactSource(
    private val database: () -> MemoraDatabase,
) : AssetMemoryFactSource {
    override suspend fun loadCurrentFacts(asset: Asset): List<AssetMemoryFact> {
        val dao = database().assetMemoryFactDao()
        val sourceId = asset.identity.sourceId.value
        val sourceAssetKey = asset.identity.sourceAssetKey.value
        val fingerprint = asset.fingerprint.value
        return when (asset.type) {
            AssetType.PDF -> buildList {
                dao.findPdfPages(sourceId, sourceAssetKey, fingerprint, PDF_SCHEMA)
                    .forEach { page ->
                        add(
                            AssetMemoryFact(
                                kind = MemoryEvidenceKind.DOCUMENT_TEXT,
                                locator = "pdf:page:${page.pageNumber}",
                                excerpt = page.pageText,
                                extractionSchemaVersion = page.schemaVersion,
                            ),
                        )
                    }
                dao.findPdfHeader(sourceId, sourceAssetKey, fingerprint, PDF_SCHEMA)
                    ?.title
                    ?.takeIf(String::isNotBlank)
                    ?.let { title ->
                        add(
                            AssetMemoryFact(
                                kind = MemoryEvidenceKind.SOURCE_METADATA,
                                locator = "pdf:title",
                                excerpt = "Title: $title",
                                extractionSchemaVersion = PDF_SCHEMA,
                            ),
                        )
                    }
                dao.findPdfMetadata(sourceId, sourceAssetKey, fingerprint, PDF_SCHEMA)
                    .forEach { metadata ->
                        add(
                            AssetMemoryFact(
                                kind = MemoryEvidenceKind.SOURCE_METADATA,
                                locator = "pdf:metadata:${metadata.metadataName}",
                                excerpt = "${metadata.metadataName}: ${metadata.metadataValue}",
                                extractionSchemaVersion = metadata.schemaVersion,
                            ),
                        )
                    }
            }

            AssetType.SCREENSHOT -> buildList {
                dao.findScreenshotOcr(
                    sourceId,
                    sourceAssetKey,
                    fingerprint,
                    ScreenshotOcrSchemaVersion.V1.value,
                )?.fullText?.takeIf(String::isNotBlank)?.let { text ->
                    add(
                        AssetMemoryFact(
                            kind = MemoryEvidenceKind.OCR_TEXT,
                            locator = "image:whole",
                            excerpt = text,
                            extractionSchemaVersion = ScreenshotOcrSchemaVersion.V1.value,
                        ),
                    )
                }
                exifFact(dao.findExif(
                    sourceId,
                    sourceAssetKey,
                    fingerprint,
                    ImageExifSchemaVersion.V1.value,
                ))?.let(::add)
            }

            AssetType.PHOTO -> buildList {
                dao.findPhotoOcr(
                    sourceId,
                    sourceAssetKey,
                    fingerprint,
                    PhotoOcrSchemaVersion.V1.value,
                )?.fullText?.takeIf(String::isNotBlank)?.let { text ->
                    add(
                        AssetMemoryFact(
                            kind = MemoryEvidenceKind.OCR_TEXT,
                            locator = "image:whole",
                            excerpt = text,
                            extractionSchemaVersion = PhotoOcrSchemaVersion.V1.value,
                        ),
                    )
                }
                exifFact(dao.findExif(
                    sourceId,
                    sourceAssetKey,
                    fingerprint,
                    ImageExifSchemaVersion.V1.value,
                ))?.let(::add)
            }

            AssetType.NOTE -> emptyList()
        }
    }

    override suspend fun findNextPendingAsset(
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Asset? = database().assetMemoryFactDao().findNextPendingAsset(
        assemblySchemaVersion = assemblySchemaVersion.value,
        pdfSchemaVersion = PDF_SCHEMA,
        screenshotOcrSchemaVersion = ScreenshotOcrSchemaVersion.V1.value,
        photoOcrSchemaVersion = PhotoOcrSchemaVersion.V1.value,
        exifSchemaVersion = ImageExifSchemaVersion.V1.value,
    )?.toDomain()?.asset

    private fun exifFact(entity: ImageExifExtractionEntity?): AssetMemoryFact? {
        entity ?: return null
        val fields = buildList {
            entity.datetimeOriginal?.takeIf(String::isNotBlank)?.let { add("Date taken: $it") }
            val camera = listOfNotNull(
                entity.make?.takeIf(String::isNotBlank),
                entity.model?.takeIf(String::isNotBlank),
            ).joinToString(" ").trim()
            if (camera.isNotBlank()) add("Camera: $camera")
            if (entity.imageWidth != null && entity.imageHeight != null) {
                add("Dimensions: ${entity.imageWidth} × ${entity.imageHeight}")
            }
            entity.orientation?.let { add("Orientation: $it") }
        }
        if (fields.isEmpty()) return null
        return AssetMemoryFact(
            kind = MemoryEvidenceKind.SOURCE_METADATA,
            locator = "exif:fields",
            excerpt = fields.joinToString("; "),
            extractionSchemaVersion = entity.schemaVersion,
        )
    }

    companion object {
        const val PDF_SCHEMA = "pdf-extraction-v1"
    }
}
