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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.memora.app.ui.privacy.ClearDerivedDataPhase
import com.memora.app.ui.privacy.ClearDerivedDataUiState
import com.memora.app.ui.privacy.ClearDerivedDataViewModel
import com.memora.app.ui.privacy.DatabaseAvailabilityPhase
import com.memora.app.ui.privacy.DatabaseAvailabilityUiState
import com.memora.app.ui.privacy.DatabaseAvailabilityViewModel
import com.memora.app.ui.search.PdfKeywordSearchCopy
import com.memora.app.ui.search.PdfKeywordSearchHighlight
import com.memora.app.ui.search.PdfKeywordSearchPhase
import com.memora.app.ui.search.PdfKeywordSearchReadinessUi
import com.memora.app.ui.search.PdfKeywordSearchUiState
import com.memora.app.ui.search.PdfKeywordSearchViewModel
import com.memora.app.application.images.ScreenshotOcrKeywordSearchHit
import com.memora.app.ui.search.ScreenshotOcrKeywordSearchCopy
import com.memora.app.ui.search.ScreenshotOcrKeywordSearchScreen
import com.memora.app.ui.search.ScreenshotOcrKeywordSearchUiState
import com.memora.app.ui.search.ScreenshotOcrKeywordSearchViewModel
import com.memora.app.ui.search.ScreenshotOriginalPreviewScreen
import com.memora.app.application.images.PhotoOcrKeywordSearchHit
import com.memora.app.ui.search.PhotoOcrKeywordSearchCopy
import com.memora.app.ui.search.PhotoOcrKeywordSearchScreen
import com.memora.app.ui.search.PhotoOcrKeywordSearchUiState
import com.memora.app.ui.search.PhotoOcrKeywordSearchViewModel
import com.memora.app.ui.search.PhotoOriginalPreviewScreen
import com.memora.app.application.notes.NotePageKeywordSearchHit
import com.memora.app.ui.search.NotePageKeywordSearchCopy
import com.memora.app.ui.search.NotePageKeywordSearchScreen
import com.memora.app.ui.search.NotePageKeywordSearchUiState
import com.memora.app.ui.search.NotePageKeywordSearchViewModel
import com.memora.app.ui.setup.MediaStoreIndexingState
import com.memora.app.ui.setup.MediaStoreSetupUiState
import com.memora.app.ui.setup.MediaStoreSetupViewModel
import com.memora.app.ui.setup.DocumentTreeConnectionState
import com.memora.app.ui.setup.DocumentTreeSetupUiState
import com.memora.app.ui.setup.DocumentTreeSetupViewModel
import com.memora.app.ui.setup.PdfFolderIndexingState
import com.memora.app.ui.setup.NotesConnectorHonestyCopy
import com.memora.app.ui.setup.NotesConnectorHonestyScreen
import com.memora.app.ui.setup.NotesConnectorUiState
import com.memora.app.ui.setup.NotesConnectorViewModel
import com.memora.app.ui.setup.PdfLocalReadingCopy
import com.memora.app.ui.setup.PdfLocalReadingState
import com.memora.app.ui.setup.PdfLocalReadingViewModel
import com.memora.app.ui.setup.ImageExifExtractUiState
import com.memora.app.ui.setup.MEDIASTORE_EXIF_EXTRACT_IN_PROGRESS_BODY
import com.memora.app.ui.setup.MEDIASTORE_INDEXING_IN_PROGRESS_BODY
import com.memora.app.ui.setup.MEDIASTORE_SCREENSHOT_OCR_IN_PROGRESS_BODY
import com.memora.app.ui.setup.MEDIASTORE_PHOTO_OCR_IN_PROGRESS_BODY
import com.memora.app.ui.setup.PDF_FOLDER_INDEXING_IN_PROGRESS_BODY
import com.memora.app.ui.setup.ScreenshotOcrExtractUiState
import com.memora.app.ui.setup.PhotoOcrExtractUiState
import com.memora.app.ui.setup.completedImageExifExtractSummary
import com.memora.app.ui.setup.completedIndexingSummary
import com.memora.app.ui.setup.completedPdfFolderIndexingSummary
import com.memora.app.ui.setup.completedScreenshotOcrExtractSummary
import com.memora.app.ui.setup.completedPhotoOcrExtractSummary
import com.memora.app.ui.setup.AssetMemorySetupCopy
import com.memora.app.ui.setup.AssetMemorySetupState
import com.memora.app.ui.setup.AssetMemorySetupViewModel
import com.memora.app.ui.setup.pdfLocalReadingBody
import com.memora.app.ui.theme.MemoraTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.Bitmap
import com.memora.app.application.documents.PdfKeywordSearchHit
import com.memora.app.ui.search.PdfOpenFeedbackUi
import com.memora.app.ui.search.PdfOriginalPreviewUi
import androidx.activity.compose.BackHandler
import java.io.BufferedReader
import java.io.InputStreamReader

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mediaStoreSetupViewModel: MediaStoreSetupViewModel by viewModels()
    private val documentTreeSetupViewModel: DocumentTreeSetupViewModel by viewModels()
    private val clearDerivedDataViewModel: ClearDerivedDataViewModel by viewModels()
    private val databaseAvailabilityViewModel: DatabaseAvailabilityViewModel by viewModels()
    private val pdfLocalReadingViewModel: PdfLocalReadingViewModel by viewModels()
    private val pdfKeywordSearchViewModel: PdfKeywordSearchViewModel by viewModels()
    private val screenshotOcrKeywordSearchViewModel: ScreenshotOcrKeywordSearchViewModel by viewModels()
    private val photoOcrKeywordSearchViewModel: PhotoOcrKeywordSearchViewModel by viewModels()
    private val notePageKeywordSearchViewModel: NotePageKeywordSearchViewModel by viewModels()
    private val assetMemorySetupViewModel: AssetMemorySetupViewModel by viewModels()
    private val notesConnectorViewModel: NotesConnectorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val setupUiState by mediaStoreSetupViewModel.uiState.collectAsState()
            val documentTreeSetupUiState by documentTreeSetupViewModel.uiState.collectAsState()
            val clearDerivedDataUiState by clearDerivedDataViewModel.uiState.collectAsState()
            val databaseAvailabilityUiState by databaseAvailabilityViewModel.uiState.collectAsState()
            val pdfLocalReadingState by pdfLocalReadingViewModel.uiState.collectAsState()
            val pdfKeywordSearchUiState by pdfKeywordSearchViewModel.uiState.collectAsState()
            val screenshotOcrKeywordSearchUiState by
                screenshotOcrKeywordSearchViewModel.uiState.collectAsState()
            val photoOcrKeywordSearchUiState by photoOcrKeywordSearchViewModel.uiState.collectAsState()
            val notePageKeywordSearchUiState by notePageKeywordSearchViewModel.uiState.collectAsState()
            val assetMemorySetupState by assetMemorySetupViewModel.uiState.collectAsState()
            val notesConnectorUiState by notesConnectorViewModel.uiState.collectAsState()

            MemoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MemoraApp(
                        databaseAvailabilityUiState = databaseAvailabilityUiState,
                        setupUiState = setupUiState,
                        documentTreeSetupUiState = documentTreeSetupUiState,
                        clearDerivedDataUiState = clearDerivedDataUiState,
                        pdfLocalReadingState = pdfLocalReadingState,
                        pdfKeywordSearchUiState = pdfKeywordSearchUiState,
                        screenshotOcrKeywordSearchUiState = screenshotOcrKeywordSearchUiState,
                        photoOcrKeywordSearchUiState = photoOcrKeywordSearchUiState,
                        notePageKeywordSearchUiState = notePageKeywordSearchUiState,
                        assetMemorySetupState = assetMemorySetupState,
                        notesConnectorUiState = notesConnectorUiState,
                        onPhotoPermissionResult = mediaStoreSetupViewModel::onPhotoPermissionResult,
                        onIndexRequested = mediaStoreSetupViewModel::onIndexRequested,
                        onImageExifExtractRequested = mediaStoreSetupViewModel::onExifExtractRequested,
                        onScreenshotOcrExtractRequested =
                            mediaStoreSetupViewModel::onScreenshotOcrExtractRequested,
                        onPhotoOcrExtractRequested = mediaStoreSetupViewModel::onPhotoOcrExtractRequested,
                        onBuildAssetMemories = assetMemorySetupViewModel::onBuildRequested,
                        onDocumentTreeReadAccessReceived =
                            documentTreeSetupViewModel::onPersistedReadAccessReceived,
                        onDocumentTreeReadAccessFailed =
                            documentTreeSetupViewModel::onPersistableReadAccessFailed,
                        onPdfIndexRequested = documentTreeSetupViewModel::onIndexRequested,
                        onAcknowledgeLocalReadingScope = pdfLocalReadingViewModel::onAcknowledgeScope,
                        onStartLocalReadingStep = pdfLocalReadingViewModel::onStart,
                        onPauseLocalReadingStep = pdfLocalReadingViewModel::onPause,
                        onResumeLocalReadingStep = pdfLocalReadingViewModel::onResume,
                        onStopLocalReadingStep = pdfLocalReadingViewModel::onStop,
                        onRetryLocalReadingStep = pdfLocalReadingViewModel::onRetry,
                        onShowLocalReadingRetryableDemo = pdfLocalReadingViewModel::onShowRetryableDemo,
                        onPdfKeywordQueryChanged = pdfKeywordSearchViewModel::onQueryChanged,
                        onPdfKeywordSearch = pdfKeywordSearchViewModel::onSearch,
                        onPdfKeywordSearchScreenVisible = pdfKeywordSearchViewModel::onScreenVisible,
                        onPdfKeywordQueryCleared = pdfKeywordSearchViewModel::onQueryCleared,
                        onPdfKeywordSearchCancelled = pdfKeywordSearchViewModel::onSearchCancelled,
                        onPdfKeywordOpenOriginal = pdfKeywordSearchViewModel::onOpenOriginalPdf,
                        onPdfKeywordOpenFeedbackDismissed =
                            pdfKeywordSearchViewModel::onOpenFeedbackDismissed,
                        onPdfKeywordPreviewClosed = pdfKeywordSearchViewModel::onOriginalPreviewClosed,
                        onScreenshotOcrKeywordQueryChanged =
                            screenshotOcrKeywordSearchViewModel::onQueryChanged,
                        onScreenshotOcrKeywordSearch = screenshotOcrKeywordSearchViewModel::onSearch,
                        onScreenshotOcrKeywordSearchScreenVisible =
                            screenshotOcrKeywordSearchViewModel::onScreenVisible,
                        onScreenshotOcrKeywordQueryCleared =
                            screenshotOcrKeywordSearchViewModel::onQueryCleared,
                        onScreenshotOcrKeywordSearchCancelled =
                            screenshotOcrKeywordSearchViewModel::onSearchCancelled,
                        onScreenshotOcrKeywordOpenOriginal =
                            screenshotOcrKeywordSearchViewModel::onOpenOriginalScreenshot,
                        onScreenshotOcrKeywordOpenFeedbackDismissed =
                            screenshotOcrKeywordSearchViewModel::onOpenFeedbackDismissed,
                        onScreenshotOcrKeywordPreviewClosed =
                            screenshotOcrKeywordSearchViewModel::onOriginalPreviewClosed,
                        onPhotoOcrKeywordQueryChanged = photoOcrKeywordSearchViewModel::onQueryChanged,
                        onPhotoOcrKeywordSearch = photoOcrKeywordSearchViewModel::onSearch,
                        onPhotoOcrKeywordSearchScreenVisible =
                            photoOcrKeywordSearchViewModel::onScreenVisible,
                        onPhotoOcrKeywordQueryCleared =
                            photoOcrKeywordSearchViewModel::onQueryCleared,
                        onPhotoOcrKeywordSearchCancelled =
                            photoOcrKeywordSearchViewModel::onSearchCancelled,
                        onPhotoOcrKeywordOpenOriginal =
                            photoOcrKeywordSearchViewModel::onOpenOriginalPhoto,
                        onPhotoOcrKeywordOpenFeedbackDismissed =
                            photoOcrKeywordSearchViewModel::onOpenFeedbackDismissed,
                        onPhotoOcrKeywordPreviewClosed =
                            photoOcrKeywordSearchViewModel::onOriginalPreviewClosed,
                        onNotePageKeywordQueryChanged =
                            notePageKeywordSearchViewModel::onQueryChanged,
                        onNotePageKeywordSearch = notePageKeywordSearchViewModel::onSearch,
                        onNotePageKeywordSearchScreenVisible =
                            notePageKeywordSearchViewModel::onScreenVisible,
                        onNotePageKeywordQueryCleared =
                            notePageKeywordSearchViewModel::onQueryCleared,
                        onNotePageKeywordSearchCancelled =
                            notePageKeywordSearchViewModel::onSearchCancelled,
                        onNotePageKeywordOpenOriginal =
                            notePageKeywordSearchViewModel::onOpenOriginalNote,
                        onNotePageKeywordOpenFeedbackDismissed =
                            notePageKeywordSearchViewModel::onOpenFeedbackDismissed,
                        onClearIndexRequested = clearDerivedDataViewModel::onClearRequested,
                        onClearIndexConfirmDismissed = clearDerivedDataViewModel::onConfirmDismissed,
                        onClearIndexConfirmed = {
                            clearDerivedDataViewModel.onClearConfirmed()
                        },
                        onClearIndexAcknowledged = {
                            clearDerivedDataViewModel.onClearedAcknowledged()
                            mediaStoreSetupViewModel.onDerivedDataCleared()
                            documentTreeSetupViewModel.onDerivedDataCleared()
                            pdfLocalReadingViewModel.onDerivedDataCleared()
                            pdfKeywordSearchViewModel.onDerivedDataCleared()
                            screenshotOcrKeywordSearchViewModel.onDerivedDataCleared()
                            photoOcrKeywordSearchViewModel.onDerivedDataCleared()
                            notePageKeywordSearchViewModel.onDerivedDataCleared()
                            assetMemorySetupViewModel.onDerivedDataCleared()
                            notesConnectorViewModel.onDerivedDataCleared()
                        },
                        onNotesDisconnect = notesConnectorViewModel::onDisconnectRequested,
                        onNotesConnect = notesConnectorViewModel::onConnectRequested,
                        onNotesDiscover = notesConnectorViewModel::onDiscoverRequested,
                        onNotesExtract = notesConnectorViewModel::onExtractRequested,
                        onNotesHonestyOpened = notesConnectorViewModel::refresh,
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
    pdfLocalReadingState: PdfLocalReadingState,
    pdfKeywordSearchUiState: PdfKeywordSearchUiState,
    screenshotOcrKeywordSearchUiState: ScreenshotOcrKeywordSearchUiState,
    photoOcrKeywordSearchUiState: PhotoOcrKeywordSearchUiState,
    notePageKeywordSearchUiState: NotePageKeywordSearchUiState,
    assetMemorySetupState: AssetMemorySetupState,
    notesConnectorUiState: NotesConnectorUiState,
    onPhotoPermissionResult: (Boolean) -> Unit,
    onIndexRequested: () -> Unit,
    onImageExifExtractRequested: () -> Unit,
    onScreenshotOcrExtractRequested: () -> Unit,
    onPhotoOcrExtractRequested: () -> Unit,
    onBuildAssetMemories: () -> Unit,
    onDocumentTreeReadAccessReceived: (String) -> Unit,
    onDocumentTreeReadAccessFailed: () -> Unit,
    onPdfIndexRequested: () -> Unit,
    onAcknowledgeLocalReadingScope: () -> Unit,
    onStartLocalReadingStep: () -> Unit,
    onPauseLocalReadingStep: () -> Unit,
    onResumeLocalReadingStep: () -> Unit,
    onStopLocalReadingStep: () -> Unit,
    onRetryLocalReadingStep: () -> Unit,
    onShowLocalReadingRetryableDemo: () -> Unit,
    onPdfKeywordQueryChanged: (String) -> Unit,
    onPdfKeywordSearch: () -> Unit,
    onPdfKeywordSearchScreenVisible: () -> Unit,
    onPdfKeywordQueryCleared: () -> Unit,
    onPdfKeywordSearchCancelled: () -> Unit,
    onPdfKeywordOpenOriginal: (PdfKeywordSearchHit) -> Unit,
    onPdfKeywordOpenFeedbackDismissed: () -> Unit,
    onPdfKeywordPreviewClosed: () -> Unit,
    onScreenshotOcrKeywordQueryChanged: (String) -> Unit,
    onScreenshotOcrKeywordSearch: () -> Unit,
    onScreenshotOcrKeywordSearchScreenVisible: () -> Unit,
    onScreenshotOcrKeywordQueryCleared: () -> Unit,
    onScreenshotOcrKeywordSearchCancelled: () -> Unit,
    onScreenshotOcrKeywordOpenOriginal: (ScreenshotOcrKeywordSearchHit) -> Unit,
    onScreenshotOcrKeywordOpenFeedbackDismissed: () -> Unit,
    onScreenshotOcrKeywordPreviewClosed: () -> Unit,
    onPhotoOcrKeywordQueryChanged: (String) -> Unit,
    onPhotoOcrKeywordSearch: () -> Unit,
    onPhotoOcrKeywordSearchScreenVisible: () -> Unit,
    onPhotoOcrKeywordQueryCleared: () -> Unit,
    onPhotoOcrKeywordSearchCancelled: () -> Unit,
    onPhotoOcrKeywordOpenOriginal: (PhotoOcrKeywordSearchHit) -> Unit,
    onPhotoOcrKeywordOpenFeedbackDismissed: () -> Unit,
    onPhotoOcrKeywordPreviewClosed: () -> Unit,
    onNotePageKeywordQueryChanged: (String) -> Unit,
    onNotePageKeywordSearch: () -> Unit,
    onNotePageKeywordSearchScreenVisible: () -> Unit,
    onNotePageKeywordQueryCleared: () -> Unit,
    onNotePageKeywordSearchCancelled: () -> Unit,
    onNotePageKeywordOpenOriginal: (NotePageKeywordSearchHit) -> Unit,
    onNotePageKeywordOpenFeedbackDismissed: () -> Unit,
    onClearIndexRequested: () -> Unit,
    onClearIndexConfirmDismissed: () -> Unit,
    onClearIndexConfirmed: () -> Unit,
    onClearIndexAcknowledged: () -> Unit,
    onNotesDisconnect: () -> Unit,
    onNotesConnect: (android.app.Activity) -> Unit,
    onNotesDiscover: () -> Unit,
    onNotesExtract: () -> Unit,
    onNotesHonestyOpened: () -> Unit,
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
            pdfLocalReadingState = pdfLocalReadingState,
            pdfKeywordSearchUiState = pdfKeywordSearchUiState,
            screenshotOcrKeywordSearchUiState = screenshotOcrKeywordSearchUiState,
            photoOcrKeywordSearchUiState = photoOcrKeywordSearchUiState,
            notePageKeywordSearchUiState = notePageKeywordSearchUiState,
            assetMemorySetupState = assetMemorySetupState,
            notesConnectorUiState = notesConnectorUiState,
            onPhotoPermissionResult = onPhotoPermissionResult,
            onIndexRequested = onIndexRequested,
            onImageExifExtractRequested = onImageExifExtractRequested,
            onScreenshotOcrExtractRequested = onScreenshotOcrExtractRequested,
            onPhotoOcrExtractRequested = onPhotoOcrExtractRequested,
            onBuildAssetMemories = onBuildAssetMemories,
            onDocumentTreeReadAccessReceived = onDocumentTreeReadAccessReceived,
            onDocumentTreeReadAccessFailed = onDocumentTreeReadAccessFailed,
            onPdfIndexRequested = onPdfIndexRequested,
            onAcknowledgeLocalReadingScope = onAcknowledgeLocalReadingScope,
            onStartLocalReadingStep = onStartLocalReadingStep,
            onPauseLocalReadingStep = onPauseLocalReadingStep,
            onResumeLocalReadingStep = onResumeLocalReadingStep,
            onStopLocalReadingStep = onStopLocalReadingStep,
            onRetryLocalReadingStep = onRetryLocalReadingStep,
            onShowLocalReadingRetryableDemo = onShowLocalReadingRetryableDemo,
            onPdfKeywordQueryChanged = onPdfKeywordQueryChanged,
            onPdfKeywordSearch = onPdfKeywordSearch,
            onPdfKeywordSearchScreenVisible = onPdfKeywordSearchScreenVisible,
            onPdfKeywordQueryCleared = onPdfKeywordQueryCleared,
            onPdfKeywordSearchCancelled = onPdfKeywordSearchCancelled,
            onPdfKeywordOpenOriginal = onPdfKeywordOpenOriginal,
            onPdfKeywordOpenFeedbackDismissed = onPdfKeywordOpenFeedbackDismissed,
            onPdfKeywordPreviewClosed = onPdfKeywordPreviewClosed,
            onScreenshotOcrKeywordQueryChanged = onScreenshotOcrKeywordQueryChanged,
            onScreenshotOcrKeywordSearch = onScreenshotOcrKeywordSearch,
            onScreenshotOcrKeywordSearchScreenVisible = onScreenshotOcrKeywordSearchScreenVisible,
            onScreenshotOcrKeywordQueryCleared = onScreenshotOcrKeywordQueryCleared,
            onScreenshotOcrKeywordSearchCancelled = onScreenshotOcrKeywordSearchCancelled,
            onScreenshotOcrKeywordOpenOriginal = onScreenshotOcrKeywordOpenOriginal,
            onScreenshotOcrKeywordOpenFeedbackDismissed =
                onScreenshotOcrKeywordOpenFeedbackDismissed,
            onScreenshotOcrKeywordPreviewClosed = onScreenshotOcrKeywordPreviewClosed,
            onPhotoOcrKeywordQueryChanged = onPhotoOcrKeywordQueryChanged,
            onPhotoOcrKeywordSearch = onPhotoOcrKeywordSearch,
            onPhotoOcrKeywordSearchScreenVisible = onPhotoOcrKeywordSearchScreenVisible,
            onPhotoOcrKeywordQueryCleared = onPhotoOcrKeywordQueryCleared,
            onPhotoOcrKeywordSearchCancelled = onPhotoOcrKeywordSearchCancelled,
            onPhotoOcrKeywordOpenOriginal = onPhotoOcrKeywordOpenOriginal,
            onPhotoOcrKeywordOpenFeedbackDismissed = onPhotoOcrKeywordOpenFeedbackDismissed,
            onPhotoOcrKeywordPreviewClosed = onPhotoOcrKeywordPreviewClosed,
            onNotePageKeywordQueryChanged = onNotePageKeywordQueryChanged,
            onNotePageKeywordSearch = onNotePageKeywordSearch,
            onNotePageKeywordSearchScreenVisible = onNotePageKeywordSearchScreenVisible,
            onNotePageKeywordQueryCleared = onNotePageKeywordQueryCleared,
            onNotePageKeywordSearchCancelled = onNotePageKeywordSearchCancelled,
            onNotePageKeywordOpenOriginal = onNotePageKeywordOpenOriginal,
            onNotePageKeywordOpenFeedbackDismissed = onNotePageKeywordOpenFeedbackDismissed,
            onClearIndexRequested = onClearIndexRequested,
            onClearIndexConfirmDismissed = onClearIndexConfirmDismissed,
            onClearIndexConfirmed = onClearIndexConfirmed,
            onClearIndexAcknowledged = onClearIndexAcknowledged,
            onNotesDisconnect = onNotesDisconnect,
            onNotesConnect = onNotesConnect,
            onNotesDiscover = onNotesDiscover,
            onNotesExtract = onNotesExtract,
            onNotesHonestyOpened = onNotesHonestyOpened,
            modifier = modifier,
        )
    }
}

@Composable
private fun MemoraAppReady(
    setupUiState: MediaStoreSetupUiState,
    documentTreeSetupUiState: DocumentTreeSetupUiState,
    clearDerivedDataUiState: ClearDerivedDataUiState,
    pdfLocalReadingState: PdfLocalReadingState,
    pdfKeywordSearchUiState: PdfKeywordSearchUiState,
    screenshotOcrKeywordSearchUiState: ScreenshotOcrKeywordSearchUiState,
    photoOcrKeywordSearchUiState: PhotoOcrKeywordSearchUiState,
    notePageKeywordSearchUiState: NotePageKeywordSearchUiState,
    assetMemorySetupState: AssetMemorySetupState,
    notesConnectorUiState: NotesConnectorUiState,
    onPhotoPermissionResult: (Boolean) -> Unit,
    onIndexRequested: () -> Unit,
    onImageExifExtractRequested: () -> Unit,
    onScreenshotOcrExtractRequested: () -> Unit,
    onPhotoOcrExtractRequested: () -> Unit,
    onBuildAssetMemories: () -> Unit,
    onDocumentTreeReadAccessReceived: (String) -> Unit,
    onDocumentTreeReadAccessFailed: () -> Unit,
    onPdfIndexRequested: () -> Unit,
    onAcknowledgeLocalReadingScope: () -> Unit,
    onStartLocalReadingStep: () -> Unit,
    onPauseLocalReadingStep: () -> Unit,
    onResumeLocalReadingStep: () -> Unit,
    onStopLocalReadingStep: () -> Unit,
    onRetryLocalReadingStep: () -> Unit,
    onShowLocalReadingRetryableDemo: () -> Unit,
    onPdfKeywordQueryChanged: (String) -> Unit,
    onPdfKeywordSearch: () -> Unit,
    onPdfKeywordSearchScreenVisible: () -> Unit,
    onPdfKeywordQueryCleared: () -> Unit,
    onPdfKeywordSearchCancelled: () -> Unit,
    onPdfKeywordOpenOriginal: (PdfKeywordSearchHit) -> Unit,
    onPdfKeywordOpenFeedbackDismissed: () -> Unit,
    onPdfKeywordPreviewClosed: () -> Unit,
    onScreenshotOcrKeywordQueryChanged: (String) -> Unit,
    onScreenshotOcrKeywordSearch: () -> Unit,
    onScreenshotOcrKeywordSearchScreenVisible: () -> Unit,
    onScreenshotOcrKeywordQueryCleared: () -> Unit,
    onScreenshotOcrKeywordSearchCancelled: () -> Unit,
    onScreenshotOcrKeywordOpenOriginal: (ScreenshotOcrKeywordSearchHit) -> Unit,
    onScreenshotOcrKeywordOpenFeedbackDismissed: () -> Unit,
    onScreenshotOcrKeywordPreviewClosed: () -> Unit,
    onPhotoOcrKeywordQueryChanged: (String) -> Unit,
    onPhotoOcrKeywordSearch: () -> Unit,
    onPhotoOcrKeywordSearchScreenVisible: () -> Unit,
    onPhotoOcrKeywordQueryCleared: () -> Unit,
    onPhotoOcrKeywordSearchCancelled: () -> Unit,
    onPhotoOcrKeywordOpenOriginal: (PhotoOcrKeywordSearchHit) -> Unit,
    onPhotoOcrKeywordOpenFeedbackDismissed: () -> Unit,
    onPhotoOcrKeywordPreviewClosed: () -> Unit,
    onNotePageKeywordQueryChanged: (String) -> Unit,
    onNotePageKeywordSearch: () -> Unit,
    onNotePageKeywordSearchScreenVisible: () -> Unit,
    onNotePageKeywordQueryCleared: () -> Unit,
    onNotePageKeywordSearchCancelled: () -> Unit,
    onNotePageKeywordOpenOriginal: (NotePageKeywordSearchHit) -> Unit,
    onNotePageKeywordOpenFeedbackDismissed: () -> Unit,
    onClearIndexRequested: () -> Unit,
    onClearIndexConfirmDismissed: () -> Unit,
    onClearIndexConfirmed: () -> Unit,
    onClearIndexAcknowledged: () -> Unit,
    onNotesDisconnect: () -> Unit,
    onNotesConnect: (android.app.Activity) -> Unit,
    onNotesDiscover: () -> Unit,
    onNotesExtract: () -> Unit,
    onNotesHonestyOpened: () -> Unit,
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
    var isShowingPdfKeywordSearch by rememberSaveable { mutableStateOf(false) }
    var isShowingScreenshotOcrKeywordSearch by rememberSaveable { mutableStateOf(false) }
    var isShowingPhotoOcrKeywordSearch by rememberSaveable { mutableStateOf(false) }
    var isShowingNotePageKeywordSearch by rememberSaveable { mutableStateOf(false) }
    var isShowingOpenSourceNotices by rememberSaveable { mutableStateOf(false) }
    var isShowingNotesHonesty by rememberSaveable { mutableStateOf(false) }

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

            isShowingNotesHonesty -> {
                LaunchedEffect(Unit) {
                    onNotesHonestyOpened()
                }
                val activity = context as ComponentActivity
                NotesConnectorHonestyScreen(
                    uiState = notesConnectorUiState,
                    onConnect = { onNotesConnect(activity) },
                    onDisconnect = onNotesDisconnect,
                    onDiscover = onNotesDiscover,
                    onExtract = onNotesExtract,
                    onBack = { isShowingNotesHonesty = false },
                    modifier = modifier,
                )
            }

            isShowingPrivacyScreen -> PrivacyScreen(
                setupUiState = setupUiState,
                onRequestPhotoAccess = { permissionLauncher.launch(requiredPermissions) },
                onStartIndexing = onIndexRequested,
                onStartExifExtract = onImageExifExtractRequested,
                onStartScreenshotOcr = onScreenshotOcrExtractRequested,
                onStartPhotoOcr = onPhotoOcrExtractRequested,
                onBack = { isShowingPrivacyScreen = false },
                modifier = modifier,
            )

            isShowingDocumentTreeScreen -> DocumentTreeSetupScreen(
                setupUiState = documentTreeSetupUiState,
                pdfLocalReadingState = pdfLocalReadingState,
                onChooseFolder = { documentTreeLauncher.launch(null) },
                onStartIndexing = onPdfIndexRequested,
                onAcknowledgeLocalReadingScope = onAcknowledgeLocalReadingScope,
                onStartLocalReadingStep = onStartLocalReadingStep,
                onPauseLocalReadingStep = onPauseLocalReadingStep,
                onResumeLocalReadingStep = onResumeLocalReadingStep,
                onStopLocalReadingStep = onStopLocalReadingStep,
                onRetryLocalReadingStep = onRetryLocalReadingStep,
                onShowLocalReadingRetryableDemo = onShowLocalReadingRetryableDemo,
                onBack = { isShowingDocumentTreeScreen = false },
                modifier = modifier,
            )

            isShowingPdfKeywordSearch -> {
                val preview = pdfKeywordSearchUiState.originalPreview
                if (preview != null) {
                    PdfOriginalPreviewScreen(
                        preview = preview,
                        onClose = onPdfKeywordPreviewClosed,
                        modifier = modifier,
                    )
                } else {
                    PdfKeywordSearchScreen(
                        uiState = pdfKeywordSearchUiState,
                        onQueryChanged = onPdfKeywordQueryChanged,
                        onQueryCleared = onPdfKeywordQueryCleared,
                        onSearch = onPdfKeywordSearch,
                        onSearchCancelled = onPdfKeywordSearchCancelled,
                        onOpenOriginalPdf = onPdfKeywordOpenOriginal,
                        onDismissOpenFeedback = onPdfKeywordOpenFeedbackDismissed,
                        onBack = { isShowingPdfKeywordSearch = false },
                        modifier = modifier,
                    )
                }
            }

            isShowingScreenshotOcrKeywordSearch -> {
                val preview = screenshotOcrKeywordSearchUiState.originalPreview
                if (preview != null) {
                    ScreenshotOriginalPreviewScreen(
                        preview = preview,
                        onClose = onScreenshotOcrKeywordPreviewClosed,
                        modifier = modifier,
                    )
                } else {
                    ScreenshotOcrKeywordSearchScreen(
                        uiState = screenshotOcrKeywordSearchUiState,
                        onQueryChanged = onScreenshotOcrKeywordQueryChanged,
                        onQueryCleared = onScreenshotOcrKeywordQueryCleared,
                        onSearch = onScreenshotOcrKeywordSearch,
                        onSearchCancelled = onScreenshotOcrKeywordSearchCancelled,
                        onOpenOriginalScreenshot = onScreenshotOcrKeywordOpenOriginal,
                        onDismissOpenFeedback = onScreenshotOcrKeywordOpenFeedbackDismissed,
                        onBack = { isShowingScreenshotOcrKeywordSearch = false },
                        modifier = modifier,
                    )
                }
            }

            isShowingPhotoOcrKeywordSearch -> {
                val preview = photoOcrKeywordSearchUiState.originalPreview
                if (preview != null) {
                    PhotoOriginalPreviewScreen(
                        preview = preview,
                        onClose = onPhotoOcrKeywordPreviewClosed,
                        modifier = modifier,
                    )
                } else {
                    PhotoOcrKeywordSearchScreen(
                        uiState = photoOcrKeywordSearchUiState,
                        onQueryChanged = onPhotoOcrKeywordQueryChanged,
                        onQueryCleared = onPhotoOcrKeywordQueryCleared,
                        onSearch = onPhotoOcrKeywordSearch,
                        onSearchCancelled = onPhotoOcrKeywordSearchCancelled,
                        onOpenOriginalPhoto = onPhotoOcrKeywordOpenOriginal,
                        onDismissOpenFeedback = onPhotoOcrKeywordOpenFeedbackDismissed,
                        onBack = { isShowingPhotoOcrKeywordSearch = false },
                        modifier = modifier,
                    )
                }
            }

            isShowingNotePageKeywordSearch -> {
                NotePageKeywordSearchScreen(
                    uiState = notePageKeywordSearchUiState,
                    onQueryChanged = onNotePageKeywordQueryChanged,
                    onQueryCleared = onNotePageKeywordQueryCleared,
                    onSearch = onNotePageKeywordSearch,
                    onSearchCancelled = onNotePageKeywordSearchCancelled,
                    onOpenOriginalNote = onNotePageKeywordOpenOriginal,
                    onDismissOpenFeedback = onNotePageKeywordOpenFeedbackDismissed,
                    onBack = { isShowingNotePageKeywordSearch = false },
                    modifier = modifier,
                )
            }

            else -> MemoraWelcomeScreen(
                assetMemorySetupState = assetMemorySetupState,
                onBuildAssetMemories = onBuildAssetMemories,
                onBeginSetup = { isShowingPrivacyScreen = true },
                onConnectPdfFolder = { isShowingDocumentTreeScreen = true },
                onFindSavedPdfText = {
                    isShowingPdfKeywordSearch = true
                    onPdfKeywordSearchScreenVisible()
                },
                onFindSavedScreenshotText = {
                    isShowingScreenshotOcrKeywordSearch = true
                    onScreenshotOcrKeywordSearchScreenVisible()
                },
                onFindSavedPhotoText = {
                    isShowingPhotoOcrKeywordSearch = true
                    onPhotoOcrKeywordSearchScreenVisible()
                },
                onFindSavedNoteText = {
                    isShowingNotePageKeywordSearch = true
                    onNotePageKeywordSearchScreenVisible()
                },
                onAboutNotesIndexing = { isShowingNotesHonesty = true },
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
private fun AssetMemorySetupCard(
    state: AssetMemorySetupState,
    onBuild: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = AssetMemorySetupCopy.TITLE,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(AssetMemorySetupCopy.BODY, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            when (state) {
                AssetMemorySetupState.Loading -> Text("Checking saved memories…")
                is AssetMemorySetupState.Building -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(AssetMemorySetupCopy.BUILDING)
                }
                is AssetMemorySetupState.Failed -> {
                    Text(AssetMemorySetupCopy.FAILED, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) {
                        Text(AssetMemorySetupCopy.BUILD_LABEL)
                    }
                }
                is AssetMemorySetupState.Ready -> {
                    Text(
                        if (state.assembledInLastRun > 0) {
                            AssetMemorySetupCopy.completed(
                                state.assembledInLastRun,
                                state.currentReadyCount,
                            )
                        } else {
                            AssetMemorySetupCopy.readiness(state.currentReadyCount)
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (state.hasMore) AssetMemorySetupCopy.CONTINUE_LABEL
                            else AssetMemorySetupCopy.BUILD_LABEL,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoraWelcomeScreen(
    assetMemorySetupState: AssetMemorySetupState,
    onBuildAssetMemories: () -> Unit,
    onBeginSetup: () -> Unit,
    onConnectPdfFolder: () -> Unit,
    onFindSavedPdfText: () -> Unit,
    onFindSavedScreenshotText: () -> Unit,
    onFindSavedPhotoText: () -> Unit,
    onFindSavedNoteText: () -> Unit,
    onAboutNotesIndexing: () -> Unit,
    onOpenSourceNotices: () -> Unit,
    onClearIndex: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .padding(vertical = 24.dp),
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
        AssetMemorySetupCard(
            state = assetMemorySetupState,
            onBuild = onBuildAssetMemories,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onFindSavedPdfText,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(PdfKeywordSearchCopy.SCREEN_TITLE)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFindSavedScreenshotText,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(ScreenshotOcrKeywordSearchCopy.SCREEN_TITLE)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFindSavedPhotoText,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(PhotoOcrKeywordSearchCopy.SCREEN_TITLE)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFindSavedNoteText,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(NotePageKeywordSearchCopy.ENTRY_LABEL)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAboutNotesIndexing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(NotesConnectorHonestyCopy.ENTRY_LABEL)
        }
        Spacer(modifier = Modifier.height(12.dp))
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
fun PdfKeywordSearchScreen(
    uiState: PdfKeywordSearchUiState,
    onQueryChanged: (String) -> Unit,
    onQueryCleared: () -> Unit,
    onSearch: () -> Unit,
    onSearchCancelled: () -> Unit,
    onOpenOriginalPdf: (PdfKeywordSearchHit) -> Unit,
    onDismissOpenFeedback: () -> Unit,
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
        Button(onClick = onBack) {
            Text(PdfKeywordSearchCopy.BACK_LABEL)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = PdfKeywordSearchCopy.SCREEN_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = PdfKeywordSearchCopy.SCOPE_BODY,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (val readiness = uiState.readiness) {
                PdfKeywordSearchReadinessUi.Loading ->
                    PdfKeywordSearchCopy.READINESS_LOADING_BODY
                PdfKeywordSearchReadinessUi.CouldNotLoad ->
                    PdfKeywordSearchCopy.READINESS_COULD_NOT_LOAD_BODY
                is PdfKeywordSearchReadinessUi.Ready ->
                    PdfKeywordSearchCopy.readinessBody(
                        pageCount = readiness.snapshot.pageCount,
                        documentCount = readiness.snapshot.documentCount,
                    )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(PdfKeywordSearchCopy.QUERY_LABEL) },
            singleLine = true,
            enabled = uiState.phase !is PdfKeywordSearchPhase.Searching &&
                uiState.openFeedback !is PdfOpenFeedbackUi.Opening,
            trailingIcon = {
                if (uiState.canClearQuery) {
                    TextButton(onClick = onQueryCleared) {
                        Text(PdfKeywordSearchCopy.CLEAR_QUERY_LABEL)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (uiState.canSubmitSearch) {
                        onSearch()
                    }
                },
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Keep both actions mounted. Swapping one button's role under a finger
        // can re-fire as Search and look like Cancel failed.
        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canSubmitSearch,
        ) {
            Text(PdfKeywordSearchCopy.SEARCH_LABEL)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSearchCancelled,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canCancelSearch,
        ) {
            Text(PdfKeywordSearchCopy.CANCEL_SEARCH_LABEL)
        }
        Spacer(modifier = Modifier.height(20.dp))
        when (val phase = uiState.phase) {
            PdfKeywordSearchPhase.Idle -> {
                if (uiState.query.isBlank()) {
                    Text(
                        text = PdfKeywordSearchCopy.EMPTY_QUERY_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                } else {
                    Unit
                }
            }
            PdfKeywordSearchPhase.EmptyQuery -> Text(
                text = PdfKeywordSearchCopy.EMPTY_QUERY_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            PdfKeywordSearchPhase.Searching -> {
                PdfKeywordStatusProgress(
                    statusText = PdfKeywordSearchCopy.SEARCHING_BODY,
                )
            }
            is PdfKeywordSearchPhase.NoMatches -> Text(
                text = PdfKeywordSearchCopy.noMatchesBody(phase.query),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            is PdfKeywordSearchPhase.NothingSavedToSearch -> Text(
                text = PdfKeywordSearchCopy.NOTHING_SAVED_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            PdfKeywordSearchPhase.SearchCouldNotFinish -> Text(
                text = PdfKeywordSearchCopy.SEARCH_COULD_NOT_FINISH_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            is PdfKeywordSearchPhase.Results -> {
                Text(
                    text = PdfKeywordSearchCopy.resultsSummary(
                        query = phase.query,
                        matchCount = phase.hits.size,
                        limitReached = phase.limitReached,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                when (val feedback = uiState.openFeedback) {
                    PdfOpenFeedbackUi.None -> Unit
                    PdfOpenFeedbackUi.Opening -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        PdfKeywordStatusProgress(
                            statusText = PdfKeywordSearchCopy.OPEN_FEEDBACK_OPENING_BODY,
                        )
                    }
                    PdfOpenFeedbackUi.SourceUnavailable,
                    PdfOpenFeedbackUi.CouldNotOpen,
                    -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (feedback) {
                                PdfOpenFeedbackUi.SourceUnavailable ->
                                    PdfKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY
                                else ->
                                    PdfKeywordSearchCopy.OPEN_FEEDBACK_COULD_NOT_OPEN_BODY
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        TextButton(onClick = onDismissOpenFeedback) {
                            Text(PdfKeywordSearchCopy.DISMISS_OPEN_FEEDBACK_LABEL)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                phase.hits.forEachIndexed { index, hit ->
                    var whyExpanded by remember(phase.query, index, hit.sourceId, hit.sourceAssetKey, hit.pageNumber) {
                        mutableStateOf(false)
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = hit.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = PdfKeywordSearchCopy.pageLabel(hit.pageNumber),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = PdfKeywordSearchHighlight.annotatedExcerpt(
                                    excerpt = hit.excerpt,
                                    query = phase.query,
                                    highlightColor = MaterialTheme.colorScheme.primary,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onOpenOriginalPdf(hit) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.openFeedback !is PdfOpenFeedbackUi.Opening,
                            ) {
                                Text(PdfKeywordSearchCopy.OPEN_ORIGINAL_PDF_LABEL)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = PdfKeywordSearchCopy.OPEN_ORIGINAL_PDF_HINT,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { whyExpanded = !whyExpanded },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (whyExpanded) {
                                        PdfKeywordSearchCopy.HIDE_WHY_LABEL
                                    } else {
                                        PdfKeywordSearchCopy.WHY_THIS_RESULT_LABEL
                                    },
                                )
                            }
                            if (whyExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = PdfKeywordSearchCopy.whyThisResultBody(
                                        query = phase.query,
                                        documentLabel = hit.label,
                                        pageNumber = hit.pageNumber,
                                        excerpt = hit.excerpt,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun PdfOriginalPreviewScreen(
    preview: PdfOriginalPreviewUi,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    val imageBitmap = remember(preview) {
        Bitmap.createBitmap(
            preview.argb8888,
            preview.widthPx,
            preview.heightPx,
            Bitmap.Config.ARGB_8888,
        ).asImageBitmap()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .padding(vertical = 24.dp),
    ) {
        Button(onClick = onClose) {
            Text(PdfKeywordSearchCopy.CLOSE_PREVIEW_LABEL)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = PdfKeywordSearchCopy.PREVIEW_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = preview.documentLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = PdfKeywordSearchCopy.previewPageCaption(
                pageNumber = preview.pageNumber,
                pageCount = preview.pageCount,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = PdfKeywordSearchCopy.PREVIEW_SCOPE_BODY,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            bitmap = imageBitmap,
            contentDescription = PdfKeywordSearchCopy.previewImageContentDescription(
                documentLabel = preview.documentLabel,
                pageNumber = preview.pageNumber,
                pageCount = preview.pageCount,
            ),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    }
}

/** Spinner + status as one polite TalkBack announcement (sighted layout unchanged). */
@Composable
private fun PdfKeywordStatusProgress(
    statusText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            contentDescription = statusText
        },
    ) {
        CircularProgressIndicator(modifier = Modifier.clearAndSetSemantics { })
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { },
        )
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
    onStartExifExtract: () -> Unit,
    onStartScreenshotOcr: () -> Unit,
    onStartPhotoOcr: () -> Unit,
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
                    Text(
                        text = MEDIASTORE_INDEXING_IN_PROGRESS_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            is MediaStoreIndexingState.COMPLETED -> {
                IndexingCompletedMessage(indexing = setupUiState.indexing)
                if (setupUiState.indexing.hasMore) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartIndexing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue indexing")
                    }
                } else if (setupUiState.indexing.discoveredAssetCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ImageExifExtractSection(
                        exifExtract = setupUiState.exifExtract,
                        onStartExifExtract = onStartExifExtract,
                    )
                    if (setupUiState.exifExtract is ImageExifExtractUiState.Completed) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ScreenshotOcrExtractSection(
                            screenshotCatalogueCount = setupUiState.screenshotCatalogueCount,
                            screenshotOcr = setupUiState.screenshotOcr,
                            onStartScreenshotOcr = onStartScreenshotOcr,
                        )
                        val screenshotsDone = setupUiState.screenshotCatalogueCount == 0 ||
                            setupUiState.screenshotOcr is ScreenshotOcrExtractUiState.Completed
                        if (screenshotsDone) {
                            Spacer(modifier = Modifier.height(16.dp))
                            PhotoOcrExtractSection(
                                photoCatalogueCount = setupUiState.photoCatalogueCount,
                                photoOcr = setupUiState.photoOcr,
                                onStartPhotoOcr = onStartPhotoOcr,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No photos or screenshots were in the permitted library, " +
                            "so there is nothing to read photo facts from yet. " +
                            "Add a photo on this device (or grant access to photos that exist), " +
                            "then tap Start indexing again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                        text = "Photo access is ready. When you start, Memora will read permitted photo metadata into its private on-device catalogue in the background. It will not open, edit, upload, or delete your photos.",
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
private fun ImageExifExtractSection(
    exifExtract: ImageExifExtractUiState,
    onStartExifExtract: () -> Unit,
) {
    when (exifExtract) {
        ImageExifExtractUiState.NotStarted -> {
            Text(
                text = "Next, Memora can read basic facts from those photos (date and camera tags when present). " +
                    "This opens permitted photos read-only. It does not read text from images or create searchable memories yet.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartExifExtract,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Read photo facts")
            }
        }

        ImageExifExtractUiState.InProgress -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = MEDIASTORE_EXIF_EXTRACT_IN_PROGRESS_BODY,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        is ImageExifExtractUiState.Completed -> {
            Text(
                text = completedImageExifExtractSummary(
                    extractedCount = exifExtract.extractedCount,
                    catalogueCount = exifExtract.catalogueCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ImageExifExtractUiState.AccessStopped -> {
            Text(
                text = "Photo access is needed before Memora can read photo facts.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        is ImageExifExtractUiState.Failed -> {
            Text(
                text = exifExtract.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartExifExtract,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Try reading photo facts again")
            }
        }
    }
}

@Composable
private fun ScreenshotOcrExtractSection(
    screenshotCatalogueCount: Int,
    screenshotOcr: ScreenshotOcrExtractUiState,
    onStartScreenshotOcr: () -> Unit,
) {
    when (screenshotOcr) {
        ScreenshotOcrExtractUiState.NotStarted -> {
            if (screenshotCatalogueCount <= 0) {
                Text(
                    text = "No screenshots were catalogued, so there is nothing to read text from yet. " +
                        "Ordinary photos are not OCR’d in this step.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Next, Memora can read text from catalogued screenshots on this phone. " +
                        "This opens those screenshots read-only and stores OCR text on-device. " +
                        "It does not open keyword search or create meaning-based memories yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartScreenshotOcr,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Read text from screenshots")
                }
            }
        }

        ScreenshotOcrExtractUiState.InProgress -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = MEDIASTORE_SCREENSHOT_OCR_IN_PROGRESS_BODY,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        is ScreenshotOcrExtractUiState.Completed -> {
            Text(
                text = completedScreenshotOcrExtractSummary(
                    extractedCount = screenshotOcr.extractedCount,
                    screenshotCatalogueCount = screenshotOcr.screenshotCatalogueCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ScreenshotOcrExtractUiState.AccessStopped -> {
            Text(
                text = "Memora could not open screenshots for text reading. " +
                    "Photo access may have been limited. You can try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartScreenshotOcr,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Try reading screenshot text again")
            }
        }

        is ScreenshotOcrExtractUiState.Failed -> {
            Text(
                text = screenshotOcr.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartScreenshotOcr,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Try reading screenshot text again")
            }
        }
    }
}

@Composable
private fun PhotoOcrExtractSection(
    photoCatalogueCount: Int,
    photoOcr: PhotoOcrExtractUiState,
    onStartPhotoOcr: () -> Unit,
) {
    when (photoOcr) {
        PhotoOcrExtractUiState.NotStarted -> {
            if (photoCatalogueCount <= 0) {
                Text(
                    "No ordinary photos were catalogued, so there is no photo text to read.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Memora can read Latin text visible in catalogued ordinary photos. " +
                        "It opens photos read-only and stores OCR text on-device. " +
                        "This is keyword text extraction, not meaning-based recall.",
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStartPhotoOcr, modifier = Modifier.fillMaxWidth()) {
                    Text("Read text from photos")
                }
            }
        }
        PhotoOcrExtractUiState.InProgress -> {
            CircularProgressIndicator()
            Text(MEDIASTORE_PHOTO_OCR_IN_PROGRESS_BODY)
        }
        is PhotoOcrExtractUiState.Completed -> Text(
            completedPhotoOcrExtractSummary(
                photoOcr.extractedCount,
                photoOcr.photoCatalogueCount,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        PhotoOcrExtractUiState.AccessStopped -> {
            Text(
                "Memora could not open photos for text reading. Check photo access and try again.",
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = onStartPhotoOcr, modifier = Modifier.fillMaxWidth()) {
                Text("Try reading photo text again")
            }
        }
        is PhotoOcrExtractUiState.Failed -> {
            Text(photoOcr.message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onStartPhotoOcr, modifier = Modifier.fillMaxWidth()) {
                Text("Try reading photo text again")
            }
        }
    }
}

@Composable
fun DocumentTreeSetupScreen(
    setupUiState: DocumentTreeSetupUiState,
    pdfLocalReadingState: PdfLocalReadingState,
    onChooseFolder: () -> Unit,
    onStartIndexing: () -> Unit,
    onAcknowledgeLocalReadingScope: () -> Unit,
    onStartLocalReadingStep: () -> Unit,
    onPauseLocalReadingStep: () -> Unit,
    onResumeLocalReadingStep: () -> Unit,
    onStopLocalReadingStep: () -> Unit,
    onRetryLocalReadingStep: () -> Unit,
    onShowLocalReadingRetryableDemo: () -> Unit,
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
                Spacer(modifier = Modifier.height(20.dp))
                PdfLocalReadingRecoveryControl(
                    state = pdfLocalReadingState,
                    onAcknowledgeScope = onAcknowledgeLocalReadingScope,
                    onStart = onStartLocalReadingStep,
                    onPause = onPauseLocalReadingStep,
                    onResume = onResumeLocalReadingStep,
                    onStop = onStopLocalReadingStep,
                    onRetry = onRetryLocalReadingStep,
                    onShowRetryableDemo = onShowLocalReadingRetryableDemo,
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
private fun PdfLocalReadingRecoveryControl(
    state: PdfLocalReadingState,
    onAcknowledgeScope: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onShowRetryableDemo: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = PdfLocalReadingCopy.SECTION_TITLE,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pdfLocalReadingBody(state),
                style = MaterialTheme.typography.bodySmall,
            )
            if (state == PdfLocalReadingState.AccessRecoveryNeeded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = PdfLocalReadingCopy.RECONNECT_HINT,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (state) {
                PdfLocalReadingState.NeedsExplanation -> Button(
                    onClick = onAcknowledgeScope,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(PdfLocalReadingCopy.CONTINUE_LABEL)
                }

                PdfLocalReadingState.Ready -> {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.START_LABEL)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onShowRetryableDemo,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.SHOW_RETRYABLE_DEMO_LABEL)
                    }
                }

                PdfLocalReadingState.InProgress -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onPause,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.PAUSE_LABEL)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.STOP_LABEL)
                    }
                }

                PdfLocalReadingState.Paused -> {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.RESUME_LABEL)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.STOP_LABEL)
                    }
                }

                PdfLocalReadingState.RetryableProblem -> {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.RETRY_LABEL)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(PdfLocalReadingCopy.STOP_LABEL)
                    }
                }

                PdfLocalReadingState.Completed -> OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(PdfLocalReadingCopy.DONE_LABEL)
                }

                PdfLocalReadingState.AccessRecoveryNeeded,
                PdfLocalReadingState.PasswordProtected,
                PdfLocalReadingState.Unavailable,
                -> OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(PdfLocalReadingCopy.BACK_TO_START_LABEL)
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
            Text(
                text = PDF_FOLDER_INDEXING_IN_PROGRESS_BODY,
                style = MaterialTheme.typography.bodyMedium,
            )
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
                    Text("Continue folder indexing")
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
