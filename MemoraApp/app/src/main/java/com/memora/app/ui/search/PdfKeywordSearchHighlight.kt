package com.memora.app.ui.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.memora.app.application.documents.PdfKeywordSearchSupport

/**
 * Builds an excerpt with the first case-insensitive query span emphasized.
 *
 * Falls back to plain text when no span is found. Does not invent matches.
 */
object PdfKeywordSearchHighlight {
    fun annotatedExcerpt(
        excerpt: String,
        query: String,
        highlightColor: Color,
    ): AnnotatedString {
        val span = PdfKeywordSearchSupport.firstMatchSpan(excerpt, query)
            ?: return AnnotatedString(excerpt)

        return buildAnnotatedString {
            append(excerpt.substring(0, span.first))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = highlightColor,
                ),
            ) {
                append(excerpt.substring(span.first, span.last + 1))
            }
            append(excerpt.substring(span.last + 1))
        }
    }
}
