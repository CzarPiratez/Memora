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
        assertTrue(line.contains("gold=-"))
        assertTrue(line.contains("collapseRank=-"))
        assertTrue(line.contains("admittedRank=-"))
        assertTrue(line.contains("rankedRank=-"))
        assertTrue(line.contains("shownRank=-"))
        assertTrue(line.contains("diagnosis=no_gold_cue"))
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
        assertEquals(MeaningSearchGoldLocator.DIAGNOSIS_NO_GOLD_CUE, snapshot.goldDiagnosis)
        assertFalse(MeaningSearchTrace.toLogLine(snapshot).contains("spelling list"))
    }

    @Test
    fun from_live_path_reports_gold_out_of_the_admitted_pool() {
        val photo = hit("class", "swimming classes.jpg", cosine = 0.9f)
        val pdf = hit("tt", "Grade-2-Swimming-TT-2026.pdf", cosine = 0.4f)
        val candidate = MeaningSearchOutcome.Matches(
            query = "when are the swimming classes",
            hits = listOf(photo),
            limitReached = true,
            model = model,
            debugTrace = MeaningSearchTrace.withPool(
                vectorsScanned = 2,
                survivedFloor = 2,
                assetsAfterCollapse = 2,
                collapseHits = listOf(photo, pdf),
                admittedHits = listOf(photo),
                poolTruncated = true,
                rawQuery = "when are the swimming classes",
            ),
        )
        val snapshot = MeaningSearchTrace.fromLivePath(
            rawQuery = "when are the swimming classes",
            candidate = candidate,
            ranked = candidate,
            shown = candidate,
            latencyMs = 9L,
        )
        val line = MeaningSearchTrace.toLogLine(snapshot)
        assertEquals("Grade-2-Swimming-TT-2026.pdf", snapshot.goldLabel)
        assertEquals("PDF", snapshot.goldAssetType)
        assertEquals(2, snapshot.goldCollapseRank)
        assertEquals(null, snapshot.goldAdmittedRank)
        assertEquals(MeaningSearchGoldLocator.DIAGNOSIS_OUT_OF_POOL, snapshot.goldDiagnosis)
        assertTrue(line.contains("gold=Grade-2-Swimming-TT-2026.pdf"))
        assertTrue(line.contains("admittedRank=-"))
        assertTrue(line.contains("diagnosis=out_of_pool"))
        assertFalse(line.contains("stored summary"))
    }

    @Test
    fun from_live_path_reports_gold_in_the_pool_but_off_the_shown_page() {
        val pdf = hit("tt", "Grade-2-Swimming-TT-2026.pdf", cosine = 0.41f)
        val photo = hit("class", "swimming classes.jpg", cosine = 0.88f)
        val candidate = MeaningSearchOutcome.Matches(
            query = "when are the swimming classes",
            hits = listOf(photo, pdf),
            limitReached = true,
            model = model,
            debugTrace = MeaningSearchTrace.withPool(
                vectorsScanned = 2,
                survivedFloor = 2,
                assetsAfterCollapse = 2,
                collapseHits = listOf(photo, pdf),
                admittedHits = listOf(photo, pdf),
                poolTruncated = false,
                rawQuery = "when are the swimming classes",
            ),
        )
        val ranked = candidate.copy(hits = listOf(photo, pdf))
        val shown = candidate.copy(hits = listOf(photo), limitReached = true)
        val snapshot = MeaningSearchTrace.fromLivePath(
            rawQuery = "when are the swimming classes",
            candidate = candidate,
            ranked = ranked,
            shown = shown,
            latencyMs = 11L,
        )
        assertEquals(2, snapshot.goldAdmittedRank)
        assertEquals(2, snapshot.goldRankedRank)
        assertEquals(null, snapshot.goldShownRank)
        assertEquals(
            MeaningSearchGoldLocator.DIAGNOSIS_IN_POOL_OFF_PAGE,
            snapshot.goldDiagnosis,
        )
    }

    @Test
    fun short_label_strips_newlines_and_caps_length() {
        val label = MeaningSearchTrace.shortLabel("  Silky\nspelling   list ".repeat(10))
        assertFalse(label.contains("\n"))
        assertTrue(label.length <= 40)
    }

    private fun hit(
        id: String,
        label: String,
        summary: String = "stored summary",
        cosine: Float = 0.8f,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId(id),
        memoryId = MemoryId("m-$id"),
        sourceId = SourceId("s"),
        sourceAssetKey = SourceAssetKey(id),
        assetType = AssetType.PDF,
        label = label,
        summaryText = summary,
        score = cosine,
        model = model,
        cosine = cosine,
    )
}
