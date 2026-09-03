package com.memora.app.domain.intelligence

/**
 * Device-facing execution tier for ADR-051 Stage A recall rerank (FC-02).
 *
 * Broader phone coverage: meaning Find never requires rerank; eligible phones receive
 * rerank via smart automatic onboarding (ADR-052), not a separate download hunt.
 */
enum class RecallRankExecutionTier {
    /** Pool up to [RecallRankDevicePolicy.FULL_POOL_SIZE]; rerank when pack installed. */
    FULL,

    /** Pool up to [RecallRankDevicePolicy.REDUCED_POOL_SIZE]; rerank when pack installed. */
    REDUCED,

    /** Skip rerank even when pack exists — identity order only. */
    IDENTITY_ONLY,
}

/**
 * Inputs for tier resolution — platform adapters supply; domain stays pure.
 */
data class RecallRankDeviceSnapshot(
    val totalRamBytes: Long?,
    val primaryAbi: String?,
    val isEmulator: Boolean,
) {
    val totalRamMegabytes: Int? =
        totalRamBytes?.let { bytes ->
            if (bytes <= 0L) null else (bytes / (1024L * 1024L)).toInt()
        }
}

object RecallRankDevicePolicy {
    const val FULL_POOL_SIZE = 40
    const val REDUCED_POOL_SIZE = 20
    const val ABSOLUTE_MAX_POOL_SIZE = 50

    const val FULL_MAX_SEQUENCE_LENGTH = 128
    const val REDUCED_MAX_SEQUENCE_LENGTH = 96

    /** Meaning Find never requires rerank; on eligible phones rerank auto-installs in unified onboarding (ADR-052). */
    const val RERANK_NOT_REQUIRED_FOR_MEANING_FIND = true

    private const val MIN_RAM_MB_FOR_REDUCED = 4_096
    private const val MIN_RAM_MB_FOR_FULL = 6_144

    fun executionTier(snapshot: RecallRankDeviceSnapshot): RecallRankExecutionTier {
        val abi = snapshot.primaryAbi?.lowercase()
        if (abi != null && abi !in SUPPORTED_PRIMARY_ABIS) {
            return RecallRankExecutionTier.IDENTITY_ONLY
        }
        val ramMb = snapshot.totalRamMegabytes
        if (ramMb == null) {
            return if (snapshot.isEmulator) {
                RecallRankExecutionTier.FULL
            } else {
                RecallRankExecutionTier.REDUCED
            }
        }
        if (ramMb < MIN_RAM_MB_FOR_REDUCED) {
            return RecallRankExecutionTier.IDENTITY_ONLY
        }
        if (ramMb >= MIN_RAM_MB_FOR_FULL) {
            return RecallRankExecutionTier.FULL
        }
        return RecallRankExecutionTier.REDUCED
    }

    fun maxPoolSize(tier: RecallRankExecutionTier): Int = when (tier) {
        RecallRankExecutionTier.FULL -> FULL_POOL_SIZE
        RecallRankExecutionTier.REDUCED -> REDUCED_POOL_SIZE
        RecallRankExecutionTier.IDENTITY_ONLY -> 0
    }

    fun maxSequenceLength(tier: RecallRankExecutionTier): Int = when (tier) {
        RecallRankExecutionTier.FULL -> FULL_MAX_SEQUENCE_LENGTH
        RecallRankExecutionTier.REDUCED -> REDUCED_MAX_SEQUENCE_LENGTH
        RecallRankExecutionTier.IDENTITY_ONLY -> 0
    }

    fun shouldAttemptRerank(
        tier: RecallRankExecutionTier,
        rerankPackInstalled: Boolean,
    ): Boolean = rerankPackInstalled && tier != RecallRankExecutionTier.IDENTITY_ONLY

    private val SUPPORTED_PRIMARY_ABIS = setOf(
        "arm64-v8a",
        "armeabi-v7a",
        "x86_64",
        "x86",
    )
}
