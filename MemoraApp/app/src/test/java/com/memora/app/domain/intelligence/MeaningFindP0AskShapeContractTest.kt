package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * P0 I1 contract: ask-shape **classes** around a domain token, not one-off
 * founder sentences. Phase exit: this generator stays green; device uses the
 * fixed 8-row checklist in MEANING_FIND_PRODUCT_SCENARIO_BAR — then MF-1.2.
 */
@RunWith(Parameterized::class)
class MeaningFindP0AskShapeContractTest(
    private val paraphrase: String,
    private val expectedContent: List<String>,
) {
    @Test
    fun paraphrase_reduces_to_the_domain_cue() {
        assertEquals(paraphrase, expectedContent, RecallQueryContentTokens.tokens(paraphrase))
    }

    companion object {
        private const val T = "silky"

        private val PREFIXES = listOf(
            "",
            "show me ",
            "show me the files with ",
            "show me the file with ",
            "get me ",
            "get me the ",
            "find ",
            "find me ",
            "please find ",
            "which file has ",
            "which document has ",
            "which pdf has ",
            "doc with ",
            "docs with ",
            "document with ",
            "pdf with ",
            "I need the ",
            "I want the ",
            "can you find ",
            "could you show me ",
            "grab me ",
            "pull up ",
            "locate the ",
            "search for ",
            "get me some e.g.s from the pdf related to ",
            "get me some egs from the pdf related to ",
            "get me some examples from the pdf regarding ",
            "give me two files with ",
            "give me three files with ",
        )

        private val SUFFIXES = listOf(
            "",
            " in it",
            " in it?",
            " please",
            " from the pdf",
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> {
            val rows = mutableListOf<Array<Any>>()
            for (prefix in PREFIXES) {
                for (suffix in SUFFIXES) {
                    val q = (prefix + T + suffix).trim()
                    rows += arrayOf(q, listOf(T))
                }
            }
            rows += arrayOf("show me the files", emptyList<String>())
            rows += arrayOf("find me some documents", emptyList<String>())
            rows += arrayOf("get me some e.g.s from the pdf", emptyList<String>())
            rows += arrayOf(
                "get me some e.g.s from the pdf related to the training project",
                listOf("training", "project"),
            )
            rows += arrayOf(
                "get me some egs from the pdf related to the training project",
                listOf("training", "project"),
            )
            rows += arrayOf(
                "give me two files with swimming timetables",
                listOf("swimming", "timetables"),
            )
            rows += arrayOf("scan", listOf("scan"))
            rows += arrayOf("which file has scan in it", listOf("scan"))
            rows += arrayOf("scan silky", listOf("scan", "silky"))
            rows += arrayOf("Show me the files with swimming timetables", listOf("swimming", "timetables"))
            return rows
        }
    }
}

class MeaningFindP0AskShapeFillerTest {
    @Test
    fun filler_catalog_has_no_domain_residue() {
        val fillers = listOf(
            "show me",
            "show me the files",
            "show me the documents",
            "find files",
            "get me a pdf",
            "please",
            "which file has",
            "give me two files",
        )
        for (q in fillers) {
            assertTrue(q, RecallQueryContentTokens.tokens(q).isEmpty())
        }
    }
}
