# Change control: UNFYND identity Step 4 — user-visible Android product-noun copy

**Date:** 2026-08-23  
**Type:** Android user-visible copy (product noun only)  
**Decision guardrails:** Public brand is UNFYND. Domain language, packages, and
persistence stay unchanged. No Step 5 class/theme renames. No `applicationId`,
namespace, `MemoraDatabase`, `memora.db`, MSAL host, Gradle identity, GitHub
badge, Grounded Answers code, or Event/Knowledge Memory implementation.

## Pre-work record

- **Requirement IDs:** P-01 (product identity on the retrieval engine the user
  sees); P-16 (recognition-first UI copy); P-19 (notes connector is not a UNFYND
  account); P-18 (scope control); A-02 (no new cloud path).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`,
  `CONTINUE`, `PRODUCT_CONTRACT`, `ARCHITECTURE`, `DECISIONS` (ADR-040), `ROADMAP`,
  `PRD_TRACEABILITY`, `LOCAL_AI_TECHNICAL_SPEC`,
  `UNFYND_IDENTITY_TRANSITION_PLAYBOOK` (Step 4 only),
  `CHANGE_CONTROL_UNFYND_IDENTITY_STEP3_CANON_OVERLAY`,
  `CHANGE_CONTROL_TEMPLATE`.
- **Current-code evidence inspected:** `strings.xml` values and resource *names*;
  `*Copy.kt` objects; Welcome/setup Compose copy in `MainActivity.kt`; discovery
  and parser `message` fields; `ClearMemoraDerivedData.APPROVED_REBUILD_MESSAGE`;
  `open_source_notices.txt` product sentences; debug SAF root title; matching
  unit `*CopyTest` and exact product-sentence tests. Class names
  (`MemoraTheme`, `MemoraApp`, `MemoraDatabase`) and `applicationId` were not
  opened for rename.
- **Open ADRs / platform limitations checked:** ADR-040 binds identity. Technical
  IDs remain deferred. Frozen architecture (Memory types, Find, Evidence Package)
  stays frozen.
- **Privacy, source-access, dependency, offline, and data-retention impact:** Copy
  only. No permission, network, or retention change.
- **Smallest safe change:** Product-as-subject user-visible strings Memora → UNFYND
  in listed Android copy and matching unit tests; notices header/product
  sentences; one Unreleased changelog bullet; this record; CONTINUE checkpoint.
- **Acceptance criteria:**
  - UI product noun is UNFYND (launcher `app_name`, Clear index / unlock /
    Welcome / Notes / Find sentences).
  - Resource names such as `clear_memora_index` unchanged.
  - Copy unit tests green for touched files.
  - No package / DB / Gradle identity edits.
  - Local commit; no push.
- **Test and emulator verification plan:** Run MemoraApp unit tests covering copy
  (`*CopyTest` plus related exact-sentence tests including
  `ClearMemoraDerivedDataCopyTest`). Emulator: **user confirms** launcher name,
  Clear UNFYND index copy, unlock copy, Welcome/Notes/Find sentences. This record
  does not claim an emulator pass.
- **User-visible quality/accessibility review plan:** Product noun replacement
  only; capability words (index, memory, evidence, Find, Why) kept. User
  emulator confirmation is the accessibility/readability gate for this step.

## Classification applied

| Occurrence class | Action |
|---|---|
| Launcher label and UI sentences about the app | UNFYND |
| Memory / Asset / Find / Evidence Package / MemoryEvidence | KEEP |
| `Theme.Memora`, `MemoraApplication`, `MemoraDatabase`, `MemoraApp` composable names | KEEP (Step 5, only if later requested) |
| `applicationId`, namespace, MSAL host `com.memora.app`, `memora.db`, Keystore aliases, WorkManager unique names, log tags, fixture filenames (`memora-open-*.pdf`, `Screenshot_memora_note.png`) | KEEP |
| XML comments and KDoc that are not user-visible | LEFT (except one KDoc line in `PdfKeywordSearchCopy` that used the same product noun as the UI strings) |
| Internal exceptions (`UserCredentialUnlockGate`, `ConversionStorageGuard`), domain `SourceCapability` require message, fixture PDF titles (“Memora page two”, “Memora synthetic café recall”) | KEEP |
| Living-canon overlay / historical ADRs / changelog history | NOT mass-rewritten |

## Delivery record

- **Files/layers changed:**
  - `MemoraApp/app/src/main/res/values/strings.xml` (values only)
  - `MemoraApp/app/src/main/res/raw/open_source_notices.txt` (header/product sentences)
  - `MemoraApp/app/src/main/java/com/memora/app/MainActivity.kt` (user-visible `text =` copy; `MemoraTheme` / `MemoraApp` / `MemoraWelcomeScreen` names restored/kept)
  - Copy objects: `IndexingSummary`, `PdfFolderIndexingSummary`, `PdfLocalReadingCopy`, `AiPackDisclosureCopy`, `NotesConnectorHonestyCopy`, `AssetMemorySetupCopy.FAILED`, keyword/meaning `*Copy.kt`
  - ViewModel user failure copy: `DocumentTreeSetupViewModel`, `MediaStoreSetupViewModel`
  - Discovery/parser user `message` fields; MSAL Connect user failure sentence
  - `ClearMemoraDerivedData.APPROVED_REBUILD_MESSAGE`
  - Debug SAF root title in `SyntheticPdfDocumentsProvider`
  - Matching unit tests (and parser androidTest expected failure message for the same user sentence)
  - `CONTINUE.md` (current checkpoint)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (one living Step 4 pointer sentence)
  - `docs/CHANGELOG.md` (one Unreleased bullet)
  - `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP4_ANDROID_COPY.md` (new)
- **Not changed:** `applicationId` / namespace; `MemoraDatabase` / `memora.db`; MSAL
  `PACKAGE_NAME`; `libs.versions.toml`; `docs/ROADMAP.md`; GitHub badge URL;
  resource IDs; Step 5 class/theme names.
- **Automated verification and result:** `:app:testDebugUnitTest` with JDK 21
  (`C:\Users\DELL\.jdks\jdk-21.0.11+10`) for the 17 copy-related classes listed
  below: **69 tests, 0 failures, 0 skipped**, BUILD SUCCESSFUL (28m 5s). Classes:
  `PdfKeywordSearchCopyTest`, `ScreenshotOcrKeywordSearchCopyTest`,
  `PhotoOcrKeywordSearchCopyTest`, `NotePageKeywordSearchCopyTest`,
  `MeaningSearchCopyTest`, `PdfLocalReadingCopyTest`, `AiPackDisclosureCopyTest`,
  `NotesConnectorHonestyCopyTest`, `AssetMemorySetupCopyTest`,
  `IndexingSummaryTest`, `PdfFolderIndexingSummaryTest`,
  `ClearMemoraDerivedDataCopyTest`, `ProcessDiscoveryResultTest`,
  `DiscoverSourcePageTest`, `IndexSafPdfFolderTest`,
  `DocumentTreeSetupViewModelTest`, `MediaStoreSetupViewModelTest`.
  Emulator instrumentation (including `PdfBoxPdfDocumentMapperIntegrationTest`)
  was not run.
- **Emulator/manual verification and result:** Not run in this checkpoint. User
  should confirm on emulator: launcher name UNFYND; Clear UNFYND index copy;
  unlock copy; Welcome / Notes / Find product sentences.
- **Failure/recovery paths verified:** Unit tests for rebuild/unlock copy
  (`ClearMemoraDerivedDataCopyTest`) and copy objects; discovery/parser
  exact-sentence unit tests where they existed.
- **Known limitation or follow-up:**
  - Playbook Step 5 optional internal names not started.
  - Internal exception strings and fixture PDF metadata still say Memora where they
    are not product launcher/UI copy.
  - `PdfKeywordSearchCopy` KDoc uses UNFYND because it described the same cap
    sentence the UI shows.
- **Documentation/traceability/ADR updates:** This record + changelog + CONTINUE.
  No new ADR.
- **Git commit:** Local checkpoint after verification (no push).
