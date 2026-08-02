package com.memora.app.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * E3 disclosure for the planned embedding AI Pack.
 * Affirmative acknowledgment only — no download, no AVAILABLE claim.
 */
@Composable
fun AiPackDisclosureScreen(
    uiState: AiPackDisclosureUiState,
    onAcknowledge: () -> Unit,
    onActivate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .padding(vertical = 24.dp),
    ) {
        OutlinedButton(onClick = onBack, enabled = !uiState.isBusy) {
            Text(AiPackDisclosureCopy.BACK_LABEL)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = AiPackDisclosureCopy.SCREEN_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = AiPackDisclosureCopy.LEAD_BODY,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Section(title = AiPackDisclosureCopy.SCOPE_TITLE, body = AiPackDisclosureCopy.SCOPE_BODY)
        Spacer(modifier = Modifier.height(20.dp))
        Section(title = AiPackDisclosureCopy.NETWORK_TITLE, body = AiPackDisclosureCopy.NETWORK_BODY)
        Spacer(modifier = Modifier.height(20.dp))
        Section(title = AiPackDisclosureCopy.SIZE_TITLE, body = AiPackDisclosureCopy.SIZE_BODY)
        Spacer(modifier = Modifier.height(20.dp))
        Section(title = AiPackDisclosureCopy.LICENSE_TITLE, body = AiPackDisclosureCopy.LICENSE_BODY)
        Spacer(modifier = Modifier.height(20.dp))
        Section(title = AiPackDisclosureCopy.STATUS_TITLE, body = uiState.statusBody)
        uiState.feedbackMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (uiState.isBusy) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        if (uiState.showAcknowledge) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAcknowledge,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(AiPackDisclosureCopy.ACKNOWLEDGE_LABEL)
            }
        }
        if (uiState.showActivate) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActivate,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(AiPackDisclosureCopy.ACTIVATE_LABEL)
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
    )
}
