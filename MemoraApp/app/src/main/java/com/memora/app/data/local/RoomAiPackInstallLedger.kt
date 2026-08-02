package com.memora.app.data.local

import com.memora.app.domain.intelligence.AiPackDisclosureSnapshot
import com.memora.app.domain.intelligence.AiPackInstallLedger
import com.memora.app.domain.intelligence.AiPackInstallLedgerEntry
import com.memora.app.domain.intelligence.AiPackInstallTransitions
import com.memora.app.domain.intelligence.AiPackManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Room-backed [AiPackInstallLedger]. Clears with Memora derived data (same DB).
 *
 * Disk work runs on [Dispatchers.IO]; no download or inference.
 */
class RoomAiPackInstallLedger(
    private val dao: () -> AiPackInstallLedgerDao,
) : AiPackInstallLedger {
    override fun entry(packId: String): AiPackInstallLedgerEntry? {
        require(packId.isNotBlank())
        return io { dao().find(packId)?.toDomain() }
    }

    override fun acknowledgeDisclosure(
        disclosure: AiPackDisclosureSnapshot,
    ): AiPackInstallLedgerEntry = io {
        val next = AiPackInstallTransitions.acknowledgeDisclosure(
            prior = dao().find(disclosure.packId)?.toDomain(),
            disclosure = disclosure,
        )
        dao().upsert(next.toEntity())
        next
    }

    override fun beginVerification(packId: String, atEpochMs: Long): AiPackInstallLedgerEntry =
        io {
            val next = AiPackInstallTransitions.beginVerification(
                prior = dao().find(packId)?.toDomain(),
                packId = packId,
                atEpochMs = atEpochMs,
            )
            dao().upsert(next.toEntity())
            next
        }

    override fun recordVerifiedActive(
        manifest: AiPackManifest,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry = io {
        val next = AiPackInstallTransitions.recordVerifiedActive(
            prior = dao().find(manifest.packId)?.toDomain(),
            manifest = manifest,
            atEpochMs = atEpochMs,
        )
        dao().upsert(next.toEntity())
        next
    }

    override fun recordVerificationFailed(
        packId: String,
        reason: String,
        retainPriorKnownGood: Boolean,
        atEpochMs: Long,
    ): AiPackInstallLedgerEntry = io {
        val next = AiPackInstallTransitions.recordVerificationFailed(
            prior = dao().find(packId)?.toDomain(),
            packId = packId,
            reason = reason,
            retainPriorKnownGood = retainPriorKnownGood,
            atEpochMs = atEpochMs,
        )
        dao().upsert(next.toEntity())
        next
    }

    private fun <T> io(block: suspend () -> T): T =
        runBlocking(Dispatchers.IO) { block() }
}
