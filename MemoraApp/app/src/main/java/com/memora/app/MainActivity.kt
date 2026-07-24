package com.memora.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memora.app.ui.privacy.ClearDerivedDataPhase
import com.memora.app.ui.privacy.ClearDerivedDataUiState
import com.memora.app.ui.privacy.ClearDerivedDataViewModel
import com.memora.app.ui.privacy.DatabaseAvailabilityPhase
import com.memora.app.ui.privacy.DatabaseAvailabilityUiState
import com.memora.app.ui.privacy.DatabaseAvailabilityViewModel
import com.memora.app.ui.setup.MediaStoreIndexingState
import com.memora.app.ui.setup.MediaStoreSetupUiState
import com.memora.app.ui.setup.MediaStoreSetupViewModel
import com.memora.app.ui.setup.DocumentTreeConnectionState
import com.memora.app.ui.setup.DocumentTreeSetupUiState
import com.memora.app.ui.setup.DocumentTreeSetupViewModel
import com.memora.app.ui.setup.PdfFolderIndexingState
import com.memora.app.ui.setup.completedIndexingSummary
import com.memora.app.ui.setup.completedPdfFolderIndexingSummary
import com.memora.app.ui.theme.MemoraTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.InputStreamReader

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mediaStoreSetupViewModel: MediaStoreSetupViewModel by viewModels()
    private val documentTreeSetupViewModel: DocumentTreeSetupViewModel by viewModels()
    private val clearDerivedDataViewModel: ClearDerivedDataViewModel by viewModels()
    private val databaseAvailabilityViewModel: DatabaseAvailabilityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val setupUiState by mediaStoreSetupViewModel.uiState.collectAsState()
            val documentTreeSetupUiState by documentTreeSetupViewModel.uiState.collectAsState()
            val clearDerivedDataUiState by clearDerivedDataViewModel.uiState.collectAsState()
            val databaseAvailabilityUiState by databaseAvailabilityViewModel.uiState.collectAsState()

            MemoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MemoraApp(
                        databaseAvailabilityUiState = databaseAvailabilityUiState,
                        setupUiState = setupUiState,
                        documentTreeSetupUiState = documentTreeSetupUiState,
                        clearDerivedDataUiState = clearDerivedDataUiState,
                        onPhotoPermissionResult = mediaStoreSetupViewModel::onPhotoPermissionResult,
                        onIndexRequested = mediaStoreSetupViewModel::onIndexRequested,
                        onDocumentTreeReadAccessReceived =
                            documentTreeSetupViewModel::onPersistedReadAccessReceived,
                        onDocumentTreeReadAccessFailed =
                            documentTreeSetupViewModel::onPersistableReadAccessFailed,
                        onPdfIndexRequested = documentTreeSetupViewModel::onIndexRequested,
                        onClearIndexRequested = clearDerivedDataViewModel::onClearRequested,
                        onClearIndexConfirmDismissed = clearDerivedDataViewModel::onConfirmDismissed,
                        onClearIndexConfirmed = {
                            clearDerivedDataViewModel.onClearConfirmed()
                        },
                        onClearIndexAcknowledged = {
                            clearDerivedDataViewModel.onClearedAcknowledged()
                            mediaStoreSetupViewModel.onDerivedDataCleared()
                            documentTreeSetupViewModel.onDerivedDataCleared()
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun MemoraApp(
    databaseAvailabilityUiState: DatabaseAvailabilityUiState,
    setupUiState: MediaStoreSetupUiState,
    documentTreeSetupUiState: DocumentTreeSetupUiState,
    clearDerivedDataUiState: ClearDerivedDataUiState,
    onPhotoPermissionResult: (Boolean) -> Unit,
    onIndexRequested: () -> Unit,
    onDocumentTreeReadAccessReceived: (String) -> Unit,
    onDocumentTreeReadAccessFailed: () -> Unit,
    onPdfIndexRequested: () -> Unit,
    onClearIndexRequested: () -> Unit,
    onClearIndexConfirmDismissed: () -> Unit,
    onClearIndexConfirmed: () -> Unit,
    onClearIndexAcknowledged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (databaseAvailabilityUiState.phase) {
        DatabaseAvailabilityPhase.Checking -> UnlockRequiredScreen(
            showProgress = true,
            modifier = modifier,
        )

        DatabaseAvailabilityPhase.WaitingForUnlock -> UnlockRequiredScreen(
            showProgress = false,
            modifier = modifier,
        )

        DatabaseAvailabilityPhase.Ready -> MemoraAppReady(
            setupUiState = setupUiState,
            documentTreeSetupUiState = documentTreeSetupUiState,
            clearDerivedDataUiState = clearDerivedDataUiState,
            onPhotoPermissionResult = onPhotoPermissionResult,
            onIndexRequested = onIndexRequested,
            onDocumentTreeReadAccessReceived = onDocumentTreeReadAccessReceived,
            onDocumentTreeReadAccessFailed = onDocumentTreeReadAccessFailed,
            onPdfIndexRequested = onPdfIndexRequested,
            onClearIndexRequested = onClearIndexRequested,
            onClearIndexConfirmDismissed = onClearIndexConfirmDismissed,
            onClearIndexConfirmed = onClearIndexConfirmed,
            onClearIndexAcknowledged = onClearIndexAcknowledged,
            modifier = modifier,
        )
    }
}

@Composable
private fun MemoraAppReady(
    setupUiState: MediaStoreSetupUiState,
    documentTreeSetupUiState: DocumentTreeSetupUiState,
    clearDerivedDataUiState: ClearDerivedDataUiState,
    onPhotoPermissionResult: (Boolean) -> Unit,
    onIndexRequested: () -> Unit,
    onDocumentTreeReadAccessReceived: (String) -> Unit,
    onDocumentTreeReadAccessFailed: () -> Unit,
    onPdfIndexRequested: () -> Unit,
    onClearIndexRequested: () -> Unit,
    onClearIndexConfirmDismissed: () -> Unit,
    onClearIndexConfirmed: () -> Unit,
    onClearIndexAcknowledged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val requiredPermissions = remember { mediaPermissionsForCurrentAndroidVersion() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        onPhotoPermissionResult(context.hasAnyPermission(requiredPermissions))
    }
    val documentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri == null) return@rememberLauncherForActivityResult

        val readPermission = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, readPermission)
        }.onSuccess {
            onDocumentTreeReadAccessReceived(treeUri.toString())
        }.onFailure {
            onDocumentTreeReadAccessFailed()
        }
    }
    var isShowingPrivacyScreen by rememberSaveable { mutableStateOf(false) }
    var isShowingDocumentTreeScreen by rememberSaveable { mutableStateOf(false) }
    var isShowingOpenSourceNotices by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onPhotoPermissionResult(context.hasAnyPermission(requiredPermissions))
    }

    when (val clearPhase = clearDerivedDataUiState.phase) {
        ClearDerivedDataPhase.Confirming -> ClearDerivedDataConfirmScreen(
            onConfirm = onClearIndexConfirmed,
            onCancel = onClearIndexConfirmDismissed,
            modifier = modifier,
        )

        ClearDerivedDataPhase.InProgress -> ClearDerivedDataProgressScreen(modifier = modifier)

        is ClearDerivedDataPhase.Cleared -> ClearDerivedDataResultScreen(
            message = clearPhase.message,
            onDone = onClearIndexAcknowledged,
            modifier = modifier,
        )

        ClearDerivedDataPhase.Failed -> ClearDerivedDataResultScreen(
            message = stringResource(R.string.clear_index_failed),
            onDone = onClearIndexConfirmDismissed,
            isError = true,
            modifier = modifier,
        )

        ClearDerivedDataPhase.Idle -> when {
            isShowingOpenSourceNotices -> OpenSourceNoticesScreen(
                onBack = { isShowingOpenSourceNotices = false },
                modifier = modifier,
            )

            isShowingPrivacyScreen -> PrivacyScreen(
                setupUiState = setupUiState,
                onRequestPhotoAccess = { permissionLauncher.launch(requiredPermissions) },
                onStartIndexing = onIndexRequested,
                onBack = { isShowingPrivacyScreen = false },
                modifier = modifier,
            )

            isShowingDocumentTreeScreen -> DocumentTreeSetupScreen(
                setupUiState = documentTreeSetupUiState,
                onChooseFolder = { documentTreeLauncher.launch(null) },
                onStartIndexing = onPdfIndexRequested,
                onBack = { isShowingDocumentTreeScreen = false },
                modifier = modifier,
            )

            else -> MemoraWelcomeScreen(
                onBeginSetup = { isShowingPrivacyScreen = true },
                onConnectPdfFolder = { isShowingDocumentTreeScreen = true },
                onOpenSourceNotices = { isShowingOpenSourceNotices = true },
                onClearIndex = onClearIndexRequested,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun UnlockRequiredScreen(
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.unlock_required_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.unlock_required_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.unlock_required_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showProgress) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MemoraWelcomeScreen(
    onBeginSetup: () -> Unit,
    onConnectPdfFolder: () -> Unit,
    onOpenSourceNotices: () -> Unit,
    onClearIndex: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Memora",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "What are you trying to remember?",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Memora will help you find photos, documents, screenshots, and notes using the details you remember.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Your privacy comes first",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We will explain and request access before reading anything on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBeginSetup,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Begin setup")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onConnectPdfFolder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.connect_pdf_folder))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenSourceNotices,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.open_source_licenses))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onClearIndex,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.clear_memora_index))
        }
    }
}

@Composable
fun ClearDerivedDataConfirmScreen(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.clear_index_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.clear_index_explanation),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.clear_index_confirm))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.clear_index_cancel))
        }
    }
}

@Composable
fun ClearDerivedDataProgressScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.clear_index_in_progress),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun ClearDerivedDataResultScreen(
    message: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.clear_index_done))
        }
    }
}

@Composable
fun OpenSourceNoticesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val noticesText = remember {
        context.resources.openRawResource(R.raw.open_source_notices).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Button(onClick = onBack) {
            Text(stringResource(R.string.back))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.open_source_licenses),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = noticesText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
fun PrivacyScreen(
    setupUiState: MediaStoreSetupUiState,
    onRequestPhotoAccess: () -> Unit,
    onStartIndexing: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your memories stay yours",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Memora needs your permission before it can help you find anything. It will never alter or delete your original files.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "What you are allowing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "- Photos and screenshots\n- Read-only access\n- Only after you choose to begin indexing",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        when (setupUiState.indexing) {
            MediaStoreIndexingState.IN_PROGRESS -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Indexing one small, read-only page of photo metadata…")
                }
            }

            is MediaStoreIndexingState.COMPLETED -> {
                IndexingCompletedMessage(indexing = setupUiState.indexing)
            }

            is MediaStoreIndexingState.FAILED -> {
                Text(
                    text = "Memora could not complete this indexing step. ${setupUiState.indexing.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartIndexing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Try again")
                }
            }

            MediaStoreIndexingState.NOT_STARTED -> {
                if (setupUiState.photoAccess.isGranted) {
                    Text(
                        text = "Photo access is ready. When you start, Memora will save one small page of permitted photo metadata to its private on-device catalogue. It will not open, edit, upload, or delete your photos.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartIndexing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Start indexing")
                    }
                } else {
                    Button(
                        onClick = onRequestPhotoAccess,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Allow photo access")
                    }
                }
            }

            MediaStoreIndexingState.ACCESS_REQUIRED,
            MediaStoreIndexingState.ACCESS_REVOKED -> {
                Text(
                    text = "Photo access is needed before Memora can index anything.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPhotoAccess,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Allow photo access")
                }
            }
        }
    }
}

@Composable
fun DocumentTreeSetupScreen(
    setupUiState: DocumentTreeSetupUiState,
    onChooseFolder: () -> Unit,
    onStartIndexing: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Connect a PDF folder",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Choose one folder that Memora may later rescan for PDFs. Android controls this permission, and Memora will keep only a private reference to the folder.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "What happens next",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "- You choose one folder\n- Access is read-only\n- This step does not open or index any PDF",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        when (val connection = setupUiState.connection) {
            DocumentTreeConnectionState.LOADING -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Checking your saved PDF folder connectionsâ€¦")
                }
            }

            DocumentTreeConnectionState.READY -> {
                Button(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Choose a PDF folder")
                }
            }

            DocumentTreeConnectionState.SAVING -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Saving your private folder connection…")
                }
            }

            is DocumentTreeConnectionState.CONNECTED -> {
                Text(
                    text = "PDF folder connected. You decide when Memora reads one small, read-only page of PDF metadata.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                PdfFolderIndexingControl(
                    indexing = setupUiState.indexing,
                    onStartIndexing = onStartIndexing,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Connect another PDF folder")
                }
            }

            is DocumentTreeConnectionState.FAILED -> {
                Text(
                    text = connection.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Choose a PDF folder")
                }
            }
        }
    }
}

@Composable
private fun PdfFolderIndexingControl(
    indexing: PdfFolderIndexingState,
    onStartIndexing: () -> Unit,
) {
    when (indexing) {
        PdfFolderIndexingState.NOT_STARTED -> Button(
            onClick = onStartIndexing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Index this folder")
        }

        PdfFolderIndexingState.IN_PROGRESS -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Indexing one small, read-only page of PDF metadataâ€¦")
        }

        is PdfFolderIndexingState.COMPLETED -> {
            Text(
                text = completedPdfFolderIndexingSummary(
                    discoveredAssetCount = indexing.discoveredAssetCount,
                    hasMore = indexing.hasMore,
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (indexing.hasMore) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartIndexing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Index next page")
                }
            }
        }

        is PdfFolderIndexingState.FAILED -> {
            Text(
                text = "Memora could not complete this PDF indexing step. ${indexing.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartIndexing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun IndexingCompletedMessage(indexing: MediaStoreIndexingState.COMPLETED) {
    Text(
        text = completedIndexingSummary(
            discoveredAssetCount = indexing.discoveredAssetCount,
            hasMore = indexing.hasMore,
            accessScope = indexing.accessScope,
        ),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private val com.memora.app.ui.setup.PhotoAccessState.isGranted: Boolean
    get() = this == com.memora.app.ui.setup.PhotoAccessState.GRANTED

private fun mediaPermissionsForCurrentAndroidVersion(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
    )

    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun Context.hasAnyPermission(permissions: Array<String>): Boolean =
    permissions.any { permission ->
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
