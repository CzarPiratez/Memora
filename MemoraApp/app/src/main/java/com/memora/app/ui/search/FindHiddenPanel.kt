package com.memora.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memora.app.application.find.FindHiddenPolicy
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.FindHiddenItem

@Composable
fun FindHiddenResultsFooter(
    rankedIdentities: Collection<AssetIdentity>,
    findHidden: FindHiddenHost,
    modifier: Modifier = Modifier,
) {
    val ranked = rankedIdentities.toSet()
    val relevant = FindHiddenPolicy.relevant(findHidden.hidden, ranked) { it.asIdentity() }
    if (relevant.isEmpty()) return
    val showIntro = relevant.size < ranked.size
    Spacer(modifier = Modifier.height(16.dp))
    FindHiddenPanel(
        hidden = relevant,
        onShowAgain = findHidden.onShowAgain,
        showIntro = showIntro,
        modifier = modifier,
    )
}

@Composable
fun FindHiddenPanel(
    hidden: List<FindHiddenItem>,
    onShowAgain: (FindHiddenItem) -> Unit,
    modifier: Modifier = Modifier,
    showIntro: Boolean = true,
) {
    if (hidden.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        if (showIntro) {
            Text(
                text = FindHiddenCopy.panelTitle(hidden.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        hidden.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        hyphens = Hyphens.None,
                        lineBreak = LineBreak.Simple,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val showAgainDescription =
                    "${FindHiddenCopy.SHOW_AGAIN_LABEL}, ${item.label}"
                TextButton(
                    onClick = { onShowAgain(item) },
                    modifier = Modifier.semantics {
                        contentDescription = showAgainDescription
                    },
                ) {
                    Text(FindHiddenCopy.SHOW_AGAIN_LABEL)
                }
            }
        }
    }
}
