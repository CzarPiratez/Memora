package com.memora.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.memora.app.domain.asset.SourceAvailabilityStatus

/**
 * The words one Find surface uses for Open-original. Held together so the
 * button below stays copy-free and each surface keeps its own wording: a note
 * that needs its OneNote connection renewed does not read like a photo whose
 * file has moved.
 */
data class FindOpenOriginalCopy(
    val openLabel: String,
    /** Short enough to sit inside the button beside a spinner. */
    val openingLabel: String,
    /** The full sentence TalkBack should hear while the open is in flight. */
    val openingAnnouncement: String,
    val sourceUnavailableBody: String,
    val couldNotOpenBody: String,
    val dismissLabel: String,
    /** Shown when the original is already known unreachable; still retries. */
    val tryOpenLabel: String = "Try Open",
) {
    init {
        require(openLabel.isNotBlank())
        require(openingLabel.isNotBlank())
        require(openingAnnouncement.isNotBlank())
        require(sourceUnavailableBody.isNotBlank())
        require(couldNotOpenBody.isNotBlank())
        require(dismissLabel.isNotBlank())
        require(tryOpenLabel.isNotBlank())
    }
}

/**
 * Open-original for one result, with that result's progress and failure shown
 * on the result itself.
 *
 * Both are deliberately in-card. Progress used to render above the search box,
 * so once the list was long enough to scroll the person tapped and watched
 * nothing happen for the seconds a Graph call takes. A failure rendered up
 * there is worse than invisible progress: the tap looks like it did nothing at
 * all, and the reason it did nothing is off-screen.
 *
 * Only [state] `OPENING` disables this button. A different card opening leaves
 * this one live, and a second tap supersedes the first in the ViewModel.
 *
 * [availability] is last-known Open reachability from a prior confirmed
 * outcome. When the original is already unreachable, the standing copy appears
 * *before* a tap so Find does not look like a live file. Try Open still
 * retries — a restored file must be able to resurrect. Dismiss is only for
 * retryable CouldNotOpen; unreachable is the card's standing truth.
 */
@Composable
fun FindOpenOriginalButton(
    copy: FindOpenOriginalCopy,
    state: FindCardOpenState,
    onOpen: () -> Unit,
    onDismissFailure: () -> Unit,
    modifier: Modifier = Modifier,
    availability: SourceAvailabilityStatus = SourceAvailabilityStatus.UNKNOWN,
    onHideFromFind: (() -> Unit)? = null,
) {
    val opening = state == FindCardOpenState.OPENING
    val standingUnreachable =
        availability == SourceAvailabilityStatus.UNREACHABLE ||
            state == FindCardOpenState.SOURCE_UNAVAILABLE
    Column(modifier = modifier.fillMaxWidth()) {
        if (standingUnreachable && !opening) {
            Text(
                text = copy.sourceUnavailableBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        val openEnabled = !opening
        val openModifier = Modifier
            .fillMaxWidth()
            .then(
                if (opening) {
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = copy.openingAnnouncement
                    }
                } else {
                    Modifier
                },
            )
        val openContent: @Composable () -> Unit = {
            if (opening) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(copy.openingLabel)
            } else {
                Text(
                    if (standingUnreachable) copy.tryOpenLabel else copy.openLabel,
                )
            }
        }
        if (standingUnreachable && !opening) {
            OutlinedButton(
                onClick = onOpen,
                enabled = openEnabled,
                modifier = openModifier,
            ) {
                openContent()
            }
        } else {
            Button(
                onClick = onOpen,
                enabled = openEnabled,
                modifier = openModifier,
            ) {
                openContent()
            }
        }

        if (state == FindCardOpenState.COULD_NOT_OPEN) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = copy.couldNotOpenBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onDismissFailure) {
                Text(copy.dismissLabel)
            }
        }
        if (standingUnreachable && !opening && onHideFromFind != null) {
            TextButton(onClick = onHideFromFind) {
                Text(FindHiddenCopy.HIDE_LABEL)
            }
        }
    }
}
