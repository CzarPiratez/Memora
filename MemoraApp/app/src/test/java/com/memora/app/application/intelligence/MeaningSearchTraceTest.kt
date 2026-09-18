package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.RecallPrecision
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningSearchTraceTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun log_line_names_authorized_fields_and_does_not_quote_excerpts() {
        val snapshot = MeaningSearchTrace.Snapshot(
            outcomeKind = "Matches",
            rawQuery = "scan silky",
            contentTokens = listOf("scan", "silky"),
            vectorsScanned = 3030,
            survivedFloor = 800,
            assetsAfterCollapse = 1603,
            admitted = 60,
            poolTruncated = true,
            admittedTopCosine = 0.81f,
            tier = "Partial",
            droppedByTier = 1,
            droppedByTierLabels = listOf("Silky spelling list.pdf"),
            shown = 8,
            shownTopCosine = 0.71f,
            capTruncated = false,
            latencyMs = 140L,
        )
        val line = MeaningSearchTrace.toLogLine(snapshot)
        assertTrue(line.contains("q=scan silky"))
        assertTrue(line.contains("tokens=scan,silky"))
        assertTrue(line.contains("scanned=3030"))
        assertTrue(line.contains("floor=800"))
        assertTrue(line.contains("collapse=1603"))
        assertTrue(line.contains("admitted=60"))
        assertTrue(line.contains("poolTruncated=true"))
        assertTrue(line.contains("tier=Partial"))
        assertTrue(line.contains("droppedByTier=1"))
        assertTrue(line.contains("dropped=[Silky spelling list.pdf]"))
        assertTrue(line.contains("shown=8"))
        assertTrue(line.contains("capTruncated=false"))
        assertTrue(line.contains("latencyMs=140"))
        assertFalse(line.contains("excerpt"))
        assertFalse(line.contains("AVAILABLE"))
    }

    @Test
    fun from_live_path_reads_pool_and_tier_drops_without_excerpts() {
        val scan = hit("scan", "Scan.pdf", "a document scan")
        val silky = hit("silky", "Silky.pdf", "silky spelling list")
        val candidate = MeaningSearchOutcome.Matches(
            query = "scan silky",
            hits = listOf(scan, silky),
            limitReached = false,
            model = model,
            debugTrace = MeaningSearchTrace.withPool(
                vectorsScanned = 4,
                survivedFloor = 2,
                assetsAfterCollapse = 2,
                admittedHits = listOf(scan, silky),
                poolTruncated = false,
            ),
        )
        val ranked = candidate.copy(
            hits = listOf(scan),
            precision = RecallPrecision.Partial(
                matched = listOf("scan"),
                missing = listOf("silky"),
            ),
            debugTrace = MeaningSearchTrace.withTierDrops(
                current = candidate.debugTrace,
                before = listOf(scan, silky),
                after = listOf(scan),
            ),
        )
        val snapshot = MeaningSearchTrace.fromLivePath(
            rawQuery = "scan silky",
            candidate = candidate,
            ranked = ranked,
            shown = ranked,
            latencyMs = 12L,
        )
        assertEquals(listOf("scan", "silky"), snapshot.contentTokens)
        assertEquals(4, snapshot.vectorsScanned)
        assertEquals(2, snapshot.admitted)
        assertEquals("Partial", snapshot.tier)
        assertEquals(1, snapshot.droppedByTier)
        assertEquals(listOf("Silky.pdf"), snapshot.droppedByTierLabels)
        assertEquals(1, snapshot.shown)
        assertFalse(MeaningSearchTrace.toLogLine(snapshot).contains("spelling list"))
    }

    @Test
    fun short_label_strips_newlines_and_caps_length() {
        val label = MeaningSearchTrace.shortLabel("  Silky\nspelling   list ".repeat(10))
        assertFalse(label.contains("\n"))
        assertTrue(label.length <= 40)
    }

    private fun hit(id: String, label: String, summary: String) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(id),
        assetType = AssetType.PDF,
        label = label,
        summaryText = summary,
        score = 0.8f,
        model = model,
        cosine = 0.8f,
    )
}
