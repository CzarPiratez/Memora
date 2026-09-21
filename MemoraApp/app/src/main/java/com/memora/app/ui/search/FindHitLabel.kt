package com.memora.app.ui.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow

/**
 * Filename on a Find card. Two lines, no mid-word hyphenation, ellipsis —
 * a long timetable name must not split ".pdf".
 */
@Composable
fun FindHitLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall.copy(
            hyphens = Hyphens.None,
            lineBreak = LineBreak.Simple,
        ),
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
