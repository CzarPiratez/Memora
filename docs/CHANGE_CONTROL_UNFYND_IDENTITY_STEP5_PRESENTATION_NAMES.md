# Change control: UNFYND identity Step 5 — presentation/entry identifier rename

**Date:** 2026-08-23  
**Type:** Presentation/entry Kotlin and theme identifiers only  
**Decision guardrails:** PascalCase `Unfynd*`, not `UNFYND*`. No persistence,
package, GitHub, Gradle folder, `applicationId`, or user-visible string edits.
No repository-wide Memora replace. Prefer not renaming `MemoraDatabase` (kept).

## Pre-work record

- **Requirement IDs:** P-17 (Hilt Application + Compose theme/entry remain the
  Android foundation; names only); P-18 (scope control); A-02 (no new cloud path).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`,
  `CONTINUE`, `PRODUCT_CONTRACT`, `ARCHITECTURE`, `DECISIONS` (ADR-040), `ROADMAP`,
  `PRD_TRACEABILITY`, `LOCAL_AI_TECHNICAL_SPEC`,
  `UNFYND_IDENTITY_TRANSITION_PLAYBOOK` (Step 5 only, narrow),
  `CHANGE_CONTROL_UNFYND_IDENTITY_STEP4_ANDROID_COPY`,
  `CHANGE_CONTROL_TEMPLATE`.
- **Current-code evidence inspected:** `MemoraApplication.kt` (Hilt
  `@HiltAndroidApp` + WorkManager `Configuration.Provider`);
  `AndroidManifest.xml` `android:name` and `@style/Theme.Memora`;
  `themes.xml`; `Theme.kt` `MemoraTheme`; `MainActivity.kt` composables
  `MemoraApp` / `MemoraAppReady` / `MemoraWelcomeScreen`. No other Kotlin
  references; no `@Preview` using those symbols. Persistence types not opened.
- **Open ADRs / platform limitations checked:** ADR-040 binds identity. Technical
  IDs remain deferred. Hilt regenerates from the new Application class name.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Identifier rename only. Encrypted index, Keystore, and MSAL host unchanged.
- **Smallest safe change:** Rename listed presentation/entry types and XML theme
  style; update manifest; CONTINUE/changelog/this record.
- **Acceptance criteria:**
  - Renamed types compile (debug).
  - No persistence/package/`applicationId` edits.
  - User-visible strings unchanged from Step 4.
  - Local commit; no push.
- **Test and emulator verification plan:** Compile debug; run
  `:app:testDebugUnitTest` (or tests broken by the rename). Do not claim emulator
  unless run; user already verified Welcome copy in Step 4.
- **User-visible quality/accessibility review plan:** N/A (no copy change).

## Classification applied

| Occurrence class | Action |
|---|---|
| `MemoraApplication` / manifest `android:name` | `UnfyndApplication` |
| `MemoraTheme` / `Theme.Memora` | `UnfyndTheme` / `Theme.Unfynd` |
| `MemoraApp`, `MemoraAppReady`, `MemoraWelcomeScreen` | `UnfyndApp`, `UnfyndAppReady`, `UnfyndWelcomeScreen` |
| `applicationId` / namespace `com.memora.app` | KEEP |
| `MemoraDatabase*` / `ClearMemoraDerivedData` / `memora.db` / Keystore | KEEP |
| MSAL host `com.memora.app` | KEEP |
| Resource IDs (`clear_memora_index`), fixture filenames, log tags | KEEP |
| `MemoraApp/` Gradle folder, GitHub, `libs.versions.toml` | KEEP |
| User-visible strings | KEEP (already UNFYND) |

## Delivery record

- **Files/layers changed:**
  - `MemoraApp/app/src/main/java/com/memora/app/UnfyndApplication.kt` (added;
    former `MemoraApplication.kt` removed)
  - `MemoraApp/app/src/main/AndroidManifest.xml`
  - `MemoraApp/app/src/main/res/values/themes.xml`
  - `MemoraApp/app/src/main/java/com/memora/app/ui/theme/Theme.kt`
  - `MemoraApp/app/src/main/java/com/memora/app/MainActivity.kt`
  - `CONTINUE.md`, `docs/PRODUCT_SOURCE_REGISTRY.md` (living Step 5 pointer),
    `docs/CHANGELOG.md`, this record
- **Not changed:** Room/DB types, `applicationId`, MSAL, strings.xml, Gradle
  identity, ROADMAP, `libs.versions.toml`.
- **Automated verification and result:** After `:app:clean`,
  `:app:compileDebugKotlin` and `:app:compileDebugUnitTestKotlin` **BUILD
  SUCCESSFUL** (5m 18s; first compile without clean failed Hilt cache looking for
  `MemoraApplication`). Then `:app:testDebugUnitTest` **360 tests, 0 failures,
  0 skipped** (1m 10s). Emulator not run.
- **Emulator/manual verification and result:** Not run in this checkpoint. User
  already verified Welcome copy in Step 4.
- **Failure/recovery paths verified:** N/A (identifier rename).
- **Known limitation or follow-up:** Identity playbook presentation names
  complete. `applicationId`, `memora.db`, MSAL host, and GitHub rename remain
  deferred to later ADRs. Historical change-control still mentions
  `MemoraApplication`.
- **Documentation/traceability/ADR updates:** This record + changelog + CONTINUE.
  No new ADR.
- **Git commit:** Local checkpoint after verification (no push).
