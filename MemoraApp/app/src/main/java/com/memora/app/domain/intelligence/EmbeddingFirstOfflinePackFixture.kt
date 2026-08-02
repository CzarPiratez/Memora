package com.memora.app.domain.intelligence

/**
 * Offline pack-container fixture for embedding-first E4a (ADR-030).
 *
 * Deterministic disposable bytes prove disclosure → verify → ACTIVE on device
 * without network and without an EmbeddingEngine. Not a real embedding model.
 */
object EmbeddingFirstOfflinePackFixture {
    const val LICENSE = "LicenseRef-Memora-Embedding-Pack-Container-Pipeline-Only"

    fun validPayload(): ByteArray {
        val prefix = "MEMORA_EMBEDDING_PACK_CONTAINER_V1\n".toByteArray(Charsets.UTF_8)
        val filler = ByteArray(4_096 - prefix.size) { index ->
            ((index * 31 + 7) % 251).toByte()
        }
        return prefix + filler
    }

    fun validManifest(): AiPackManifest {
        val payload = validPayload()
        return AiPackManifest(
            packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID,
            capability = EmbeddingFirstAiPackTrack.capability,
            model = ModelVersionIdentity(
                modelId = EmbeddingFirstAiPackTrack.PLANNED_MODEL_ID,
                version = EmbeddingFirstAiPackTrack.PLANNED_MODEL_VERSION,
            ),
            compatibleAppVersions = "1.0.0+",
            compatibleSchemaVersions = "memory-schema-1",
            downloadSizeBytes = payload.size.toLong(),
            storageRequirementBytes = payload.size.toLong() + 1_024L,
            integrityHash = AiPackIntegrity.sha256Hex(payload),
            license = LICENSE,
            installationState = AiPackInstallState.NOT_INSTALLED,
        )
    }

    fun disclosure(atEpochMs: Long): AiPackDisclosureSnapshot {
        val manifest = validManifest()
        return AiPackDisclosureSnapshot(
            packId = manifest.packId,
            capability = manifest.capability,
            model = manifest.model,
            downloadSizeBytes = manifest.downloadSizeBytes,
            storageRequirementBytes = manifest.storageRequirementBytes,
            license = manifest.license,
            disclosedAtEpochMs = atEpochMs,
        )
    }
}
