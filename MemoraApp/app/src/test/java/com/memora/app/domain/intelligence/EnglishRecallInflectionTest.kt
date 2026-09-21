package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishRecallInflectionTest {
    @Test
    fun timetable_matches_timetables_both_directions() {
        val haystackSingular = "swimming timetable for year 4"
        val haystackPlural = "swimming timetables for year 4"
        assertTrue(EnglishRecallInflection.occursAsWholeWord(haystackSingular, "timetables"))
        assertTrue(EnglishRecallInflection.occursAsWholeWord(haystackPlural, "timetable"))
        assertTrue(EnglishRecallInflection.occursAsWholeWord(haystackSingular, "timetable"))
    }

    @Test
    fun a_named_digit_matches_as_a_whole_token() {
        assertTrue(EnglishRecallInflection.occursAsWholeWord("grade-2-swimming timetable", "2"))
        assertFalse(EnglishRecallInflection.occursAsWholeWord("grade 3 boys notice", "2"))
        assertEquals(setOf("2"), EnglishRecallInflection.wholeWordVariants("2"))
    }

    @Test
    fun does_not_synonym_silky_to_silk() {
        assertFalse(EnglishRecallInflection.occursAsWholeWord("soft silk fabric", "silky"))
        assertTrue(EnglishRecallInflection.wholeWordVariants("silky").contains("silky"))
        assertFalse(EnglishRecallInflection.wholeWordVariants("silky").contains("silk"))
    }
}
