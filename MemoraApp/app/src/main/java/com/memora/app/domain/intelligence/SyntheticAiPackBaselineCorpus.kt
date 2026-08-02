package com.memora.app.domain.intelligence

/**
 * Repository-owned synthetic AI Pack corpus for measured baselines (L1).
 *
 * Bytes are deterministic and disposable. They are not a real model and must never
 * be presented as Local Intelligence AVAILABLE.
 */
object SyntheticAiPackBaselineCorpus {
    const val CORPUS_ID = "synthetic-ai-pack-baseline-v1"
    const val PACK_ID = "memora.synthetic.vision.baseline.v1"
    const val DEVICE_TIER_JVM_UNIT = "jvm_unit"
    const val DEVICE_TIER_EMULATOR_MEDIUM_PHONE = "emulator_medium_phone"

    /** Deterministic payload used for successful integrity verification. */
    fun validPayload(): ByteArray {
        // Fixed prefix keeps the fixture recognizable in reviews without logging
        // user content. Length is stable so disclosed download size stays honest.
        val prefix = "MEMORA_SYNTHETIC_AI_PACK_V1\n".toByteArray(Charsets.UTF_8)
        val filler = ByteArray(4_096 - prefix.size) { index ->
            ((index * 17 + 41) % 251).toByte()
        }
        return prefix + filler
    }

    fun validManifest(): AiPackManifest {
        val payload = validPayload()
        return AiPackManifest(
            packId = PACK_ID,
            capability = CapabilityId.VISION,
            model = ModelVersionIdentity(
                modelId = "synthetic-vision-baseline",
                version = "0.0.1-test",
            ),
            compatibleAppVersions = "debug+",
            compatibleSchemaVersions = "memory-schema-1",
            downloadSizeBytes = payload.size.toLong(),
            storageRequirementBytes = payload.size.toLong() + 1_024L,
            integrityHash = AiPackIntegrity.sha256Hex(payload),
            license = "LicenseRef-Memora-Synthetic-Test-Only",
            installationState = AiPackInstallState.NOT_INSTALLED,
        )
    }

    /** Same length as [validPayload] but one byte flipped so the hash fails. */
    fun corruptedPayload(): ByteArray {
        val bytes = validPayload()
        bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 0x01).toByte()
        return bytes
    }
}
