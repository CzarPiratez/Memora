package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.memory.CanonicalRecallTestFixtures
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract §3.4: one Why shape for every asset type and both retrieval paths.
 * The panel has three slots (relevance, optional cited line, how-found).
 * Keyword cards already show the excerpt, so that slot stays empty (U5 / D-17).
 */
class WhyPresentationShapeContractTest {
    private val model = ModelVersionIdentity("m", "1")

    @Test
    fun keyword_why_is_the_same_shape_for_every_asset_type() {
        for (type in AssetType.entries) {
            val result = CanonicalRecallTestFixtures.keywordRecall(
                assetType = type,
                label = labelFor(type),
                excerpt = "Café memory: meet Mira at 10:30…",
                pageNumber = if (type == AssetType.PDF) 2 else null,
            )
            val why = CanonicalRecallWhyCopy.present(result, "meet mira")
            assertEquals(type.name, "You asked about ", why.askPrefix)
            assertEquals(type.name, "\"meet mira\"", why.ask)
            assertEquals(type.name, "Those words are ", why.subjectPrefix)
            assertEquals(type.name, "in this file's saved text", why.subject)
            assertNull(type.name, why.citedLine)
            assertEquals(type.name, CanonicalRecallWhyCopy.FOUND_BY_KEYWORD, why.howFound)
            assertFalse(type.name, why.spokenText.contains(result.label))
            assertFalse(type.name, why.spokenText.contains(result.excerpt))
            assertFalse(type.name, why.spokenText.contains("Page "))
            assertFalse(type.name, why.spokenText.contains("Why this result?"))
            assertFalse(type.name, why.spokenText.contains("Matched "))
            assertFalse(type.name, why.spokenText.contains("Has \""))
        }
    }

    @Test
    fun meaning_why_is_the_same_shape_for_every_asset_type() {
        for (type in AssetType.entries) {
            val hit = meaningHit(
                type = type,
                label = labelFor(type),
                summary = "Hotel confirmation near the cafe district for Mira",
                boosted = type == AssetType.PDF,
            )
            val why = CanonicalRecallWhyCopy.present(hit, "mira")
            assertEquals(type.name, "You asked about ", why.askPrefix)
            assertEquals(type.name, "a mira", why.ask)
            assertEquals(type.name, "This file is ", why.subjectPrefix)
            assertTrue(type.name, why.subject.isNotBlank())
            assertNotNull(type.name, why.citedLine)
            assertTrue(type.name, why.citedLine!!.contains("Mira", ignoreCase = true))
            assertEquals(
                type.name,
                if (hit.evidenceTokenBoosted) {
                    CanonicalRecallWhyCopy.FOUND_BY_MEANING_WITH_WORD_ASSIST
                } else {
                    CanonicalRecallWhyCopy.FOUND_BY_MEANING
                },
                why.howFound,
            )
            assertTrue(type.name, why.howFound.startsWith("Found by meaning"))
            assertFalse(type.name, why.spokenText.contains("Page "))
            assertFalse(type.name, why.spokenText.contains("Has \""))
            assertFalse(type.name, why.spokenText.contains("Does not have"))
            assertFalse(type.name, why.spokenText.contains("Why this result?"))
            assertFalse(type.name, why.spokenText.contains("Score reflects"))
        }
    }

    @Test
    fun spoken_text_is_the_three_slots_in_order() {
        val why = WhyPresentation(
            askPrefix = "You asked about ",
            ask = "\"silky\"",
            subjectPrefix = "That word is ",
            subject = "in this file's saved text",
            citedLine = null,
            howFound = CanonicalRecallWhyCopy.FOUND_BY_KEYWORD,
        )
        assertEquals(
            "You asked about \"silky\". That word is in this file's saved text. " +
                CanonicalRecallWhyCopy.FOUND_BY_KEYWORD,
            why.spokenText,
        )
        val withCite = why.copy(citedLine = "Irregular consonants silky wreck")
        assertTrue(withCite.spokenText.contains("\"Irregular consonants silky wreck\""))
        assertTrue(withCite.spokenText.indexOf("You asked") < withCite.spokenText.indexOf("Irregular"))
        assertTrue(
            withCite.spokenText.indexOf("Irregular") <
                withCite.spokenText.indexOf(CanonicalRecallWhyCopy.FOUND_BY_KEYWORD),
        )
    }

    private fun labelFor(type: AssetType): String = when (type) {
        AssetType.PDF -> "memora-persist-fixture.pdf"
        AssetType.PHOTO -> "receipt.jpg"
        AssetType.SCREENSHOT -> "Screenshot_memora_note.png"
        AssetType.NOTE -> "Ideas"
    }

    private fun meaningHit(
        type: AssetType,
        label: String,
        summary: String,
        boosted: Boolean,
    ) = MeaningSearchHit(
        revisionId = MemoryRevisionId("rev"),
        memoryId = MemoryId("mem"),
        sourceId = SourceId("src"),
        sourceAssetKey = SourceAssetKey("asset"),
        assetType = type,
        label = label,
        summaryText = summary,
        score = 0.5f,
        model = model,
        citedPdfPageNumber = if (type == AssetType.PDF) 1 else null,
        rankedPdfPageNumber = if (type == AssetType.PDF) 3 else null,
        evidenceTokenBoosted = boosted,
    )
}
