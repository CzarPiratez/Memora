package com.memora.app.domain.intelligence

/**
 * Planned Local-AI benchmark metric identifiers (Spec §11).
 *
 * Presence of an ID does not authorize a numeric release claim.
 */
enum class LocalAiBenchmarkMetricId {
    RECALL_AT_K,
    UNSUPPORTED_CLAIM_RATE,
    FALSE_LINK_RATE,
    EXPLANATION_COVERAGE,
    CALIBRATION_ERROR_RATE,
    SEARCH_LATENCY_MS,
    PER_ASSET_UNDERSTANDING_MS,
    PACK_DOWNLOAD_BYTES,
    PACK_ON_DISK_BYTES,
    PEAK_RSS_BYTES,
    BATTERY_CHARGE_DELTA_UAH,
    OFFLINE_CORE_PATH_OK,
    PACK_INTEGRITY_FAILURE_HANDLED,
}

/**
 * A measured sample may become a release claim only when evidence is attached.
 */
data class LocalAiBenchmarkClaim(
    val metric: LocalAiBenchmarkMetricId,
    val deviceTierId: String,
    val fixtureCorpusId: String,
    val hasMeasuredEvidence: Boolean,
) {
    init {
        require(deviceTierId.isNotBlank()) { "A benchmark claim needs a device tier id." }
        require(fixtureCorpusId.isNotBlank()) { "A benchmark claim needs a fixture corpus id." }
    }

    fun mayPublishAsReleasePromise(): Boolean = hasMeasuredEvidence
}

/**
 * Spec §11 rule: no arbitrary latency/battery/storage number without evidence.
 */
object LocalAiBenchmarkRules {
    fun allowsUnmeasuredReleasePromise(): Boolean = false
}
