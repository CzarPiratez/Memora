package com.memora.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * The one Why control and panel used by every Find result card.
 *
 * Identical toggle, colours, order, and typography on PDF, photo, screenshot,
 * note, keyword, and meaning — the divergence this replaces was five cards
 * each styling their own explanation.
 *
 * [presentation] is a factory, not a value: a result list can hold twenty
 * cards, and building an explanation walks stored text, so the work happens
 * only for the card the person actually expanded.
 */
@Composable
fun WhyDisclosure(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    presentation: () -> WhyPresentation,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (expanded) {
                    CanonicalRecallWhyCopy.HIDE_WHY_LABEL
                } else {
                    CanonicalRecallWhyCopy.WHY_THIS_RESULT_LABEL
                },
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            WhyPanel(presentation())
        }
    }
}

/**
 * Why as a bordered panel: relevance first, the stored line that supports it
 * next when the card is not already showing it, then how it was found.
 *
 * Emphasis is carried by weight and by the words themselves, never by colour
 * alone.
 */
@Composable
private fun WhyPanel(presentation: WhyPresentation) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = presentation.spokenText
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(colors.primary),
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append(presentation.askPrefix)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(presentation.ask)
                        }
                        append(". ")
                        append(presentation.subjectPrefix)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(presentation.subject)
                        }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                presentation.citedLine?.let { cited ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$cited\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.82f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = presentation.howFound,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer.copy(alpha = 0.82f),
                )
            }
        }
    }
}
