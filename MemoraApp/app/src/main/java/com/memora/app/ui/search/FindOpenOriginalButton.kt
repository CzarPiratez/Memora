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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

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
) {
    init {
        require(openLabel.isNotBlank())
        require(openingLabel.isNotBlank())
        require(openingAnnouncement.isNotBlank())
        require(sourceUnavailableBody.isNotBlank())
        require(couldNotOpenBody.isNotBlank())
        require(dismissLabel.isNotBlank())
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
 */
@Composable
fun FindOpenOriginalButton(
    copy: FindOpenOriginalCopy,
    state: FindCardOpenState,
    onOpen: () -> Unit,
    onDismissFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opening = state == FindCardOpenState.OPENING
    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = onOpen,
            enabled = !opening,
            modifier = Modifier
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
                ),
        ) {
            if (opening) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(copy.openingLabel)
            } else {
                Text(copy.openLabel)
            }
        }

        val failureBody = when (state) {
            FindCardOpenState.SOURCE_UNAVAILABLE -> copy.sourceUnavailableBody
            FindCardOpenState.COULD_NOT_OPEN -> copy.couldNotOpenBody
            FindCardOpenState.IDLE, FindCardOpenState.OPENING -> null
        }
        if (failureBody != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = failureBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onDismissFailure) {
                Text(copy.dismissLabel)
            }
        }
    }
}
