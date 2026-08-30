# Core / App separation — execution plan

**Status:** Phase 1 complete; Phase 2 not started  
**Updated:** 2026-08-30  
**Authority:** `docs/OPEN_SOURCE_COMMERCIAL_STRATEGY.md` §18–24; ADR-047 (Class A);
ADR-046 (Core noun); Architecture Freeze (dependency inward).  
**Does not authorize:** Gradle module split, public Core source release, Class B,
repository rename, MIG-* work, or Freeze reopen. Each phase needs its own
change-control record and ADR where required.

---

## 1. Purpose

UNFYND **Core** is on-device memory and intelligence **infrastructure**. **UNFYND
App** is a product surface built on Core. Today both live in one Android Gradle
project (`MemoraApp/`, single `:app` module). Class A already opens **contracts**
(`public/unfynd-core/` → GitHub `unfynd-core`). This plan describes how to
**prepare**, **modularize**, and **optionally publish** runnable Core without
distorting architecture or claiming full open source before gates are met.

**You do not need two daily builds today.** One monorepo is correct until module
boundaries are clean and an ADR defines the public Core code boundary.

---

## 2. Current state (baseline)

| Artifact | Location | Open today? |
|---|---|---|
| Core vision + contracts (Class A) | `public/unfynd-core/` | Yes (Apache-2.0 docs) |
| Runnable Core + App | `MemoraApp/app/` single module | No (private Memora repo) |
| Package layout (approx. Kotlin files) | `domain` 49 · `application` 62 · `data` 117 · `ui` 37 · `work` 21 | — |

**Dependency health (2026-08-30):**

- `domain/` → no imports from `ui/`, `data/`, or `application/` (good).
- `application/` → mostly depends on `domain/`; **some use cases import `data/`**
  directly (e.g. PDF open, OneNote, MediaStore) — must be refactored before Core
  modules compile without the App.
- `ui/` → depends on `application/` (correct for product).
- `work/` → Android workers; product/scheduling layer (stays with App or
  `core-android` adapter boundary).

**Recall boundary (shipped):** thin `CanonicalRecall` façade; L7/L8 still outside
until MIG-07B. Live/Dual **N = 2**.

---

## 3. Target end state

```text
┌─────────────────────────────────────────────────────────────┐
│  unfynd-app-android (private) — product surfaces            │
│  ui · setup · product ViewModels · branding · App DI glue     │
└───────────────────────────┬─────────────────────────────────┘
                            │ depends on
┌───────────────────────────▼─────────────────────────────────┐
│  unfynd-core-android (optional open) — platform adapters      │
│  Room · MediaStore · SAF · WorkManager · MSAL notes · security  │
└───────────────────────────┬─────────────────────────────────┘
                            │ depends on
┌───────────────────────────▼─────────────────────────────────┐
│  unfynd-core (candidate open) — portable Core                   │
│  domain · application use cases · recall/memory orchestration   │
│  capability interfaces + reference deterministic implementations  │
└─────────────────────────────────────────────────────────────┘

Public repo `unfynd-core` today = Class A docs only.
Future: same repo (or sibling) publishes versioned Core library artifacts.
```

**Repositories (illustrative, not decided):**

| Repo | Contents |
|---|---|
| `unfynd-core` (public) | Class A today; later Core library source + releases |
| `Memora` (private) | App + Android glue until App moves to `unfynd-app-*` |

Monorepo-first is allowed: multiple Gradle modules inside `MemoraApp/` with only
Core modules published externally.

---

## 4. Principles (non-negotiable)

1. **Dependencies flow inward:** `ui` → `application` → `domain` ← `data` implements ports.
2. **No new Find paths** outside Canonical Recall convergence (`LEGACY_RECALL_SURFACE` shrink-only).
3. **Open boundary = explicit ADR**, not “we pushed the whole app.”
4. **Class A ≠ Class B:** contracts can lead; runnable Core source needs IP,
   license, security, and quality gates (`OPEN_SOURCE_COMMERCIAL_STRATEGY` §23).
5. **Proprietary by default until listed:** AI Pack weights/manifests, tuning
   corpora, evaluation fixtures, keystores, MSAL secrets, commercial extensions.
6. **Audit before split:** no repository surgery for licensing convenience alone
   (strategy §18).

---

## 5. Open-boundary decision matrix (for a future ADR)

When “fully open Core” is requested, decide **per bucket**:

| Bucket | Default | Notes |
|---|---|---|
| Domain models (`Memory`, `Asset`, evidence classes) | **Open candidate** | Align with Class A `SPEC.md` |
| Capability **interfaces** (`MemoryBuilder`, `EmbeddingEngine`, seams) | **Open candidate** | Interfaces in `LocalIntelligenceEngines.kt` family |
| Deterministic reference impls (`DeterministicMemoryBuilder`) | **Open candidate** | No model weights |
| `CanonicalRecall`, `SearchMemoryEvidence` (use cases) | **Open candidate** | After MIG-07B convergence |
| Room schema + DAOs | **Split decision** | Often `core-android` or stay private initially |
| MediaPipe / embedding runtime | **Usually private** | Or interface open, impl proprietary |
| AI Pack store, ledger, payloads | **Private** | ADR-047 / ROADMAP-OPEN |
| Benchmark / synthetic corpora in repo | **Private** | ADR-048 synthetic only in Class A pack |
| Compose UI, setup flows, copy | **Private** (App) | Product |
| OneNote / MSAL connector | **Private** (App) | Product integration |

---

## 6. Phased execution

### Phase 0 — Done (baseline)

- [x] Class A pack published (`public/unfynd-core/`, GitHub `unfynd-core`)
- [x] App vs Core narrative in README / APPLICATIONS
- [x] Thin `CanonicalRecall` KEYWORD façade (`CHANGE_CONTROL_CANONICAL_RECALL_THIN_FACADE`)

**Exit:** Contracts public; implementation still monolithic.

---

### Phase 1 — Boundary hygiene (monorepo, same `:app` module)

**Goal:** Make packages enforceable before Gradle modules.

| Step | Work | Verification |
|---|---|---|
| 1.1 | Inventory `application/` → `data/` imports; list each as **port violation** | Spreadsheet or section in this doc |
| 1.2 | For each violation: introduce `domain` port or move orchestration behind repository interface already in `domain` | Unit tests; no `application` → `data` for new code |
| 1.3 | Document **Core package allowlist** (paths that belong to portable Core) | Review against Class A seams |
| 1.4 | Add optional CI script: fail if `domain` imports `android.*` or `data`/`ui` | **Done** — `scripts/check-domain-layer-purity.sh` + CI |

**Suggested Core package allowlist (portable):**

```text
com.memora.app.domain.asset.*
com.memora.app.domain.memory.*
com.memora.app.domain.intelligence.*   # interfaces + pure logic only
com.memora.app.domain.extraction.*     # models + persistence ports
com.memora.app.domain.discovery.*      # ports
com.memora.app.application.memory.*
com.memora.app.application.discovery.* # after port cleanup
com.memora.app.application.intelligence.* # recall/index use cases; not App-only open flows
```

**Explicitly App / Android (not portable Core):**

```text
com.memora.app.ui.*
com.memora.app.work.*
com.memora.app.data.*
com.memora.app.MainActivity, UnfyndApplication
application/notes/* (OneNote product connector)
application/documents/Open*, RunPdfLocalReading* (platform viewers)
```

**Authorization:** Change-control doc; no ADR unless scope grows.  
**Exit:** `application` does not import `data` types (only `domain` ports).

#### 6.1 Phase 1.1 inventory (`application` → `data`, main source)

**Guard:** `scripts/check-application-layer-boundaries.sh` (shrink-only allowlist).  
**Change control:** `CHANGE_CONTROL_CORE_APP_PHASE1_BOUNDARY_HYGIENE.md`.

| File | Data imports | Bucket | Remediation |
|---|---|---|---|
| `OpenPersistedPhotoForViewing` | — | App viewer | **Done** — uses `ImageLibraryDiscoverySource` |
| `OpenPersistedScreenshotForViewing` | — | App viewer | **Done** — uses `ImageLibraryDiscoverySource` |
| `RunPendingPhotoOcrExtract` | — | Core extract | **Done** — domain readers + `PhotoOcrExtractionPersistence` |
| `RunPendingScreenshotOcrExtract` | — | Core extract | **Done** — domain readers + `ScreenshotOcrExtractionPersistence` |
| `RunPendingImageExifExtract` | — | Core extract | **Done** — domain readers + `ImageExifExtractionPersistence` |
| `OpenPersistedPdfForViewing` | — | App viewer | **Done** — `PdfReadOnlyDescriptorAccess` |
| `RunPdfLocalReadingStatusCheck` | — | Core extract | **Done** — `PdfIsolatedLocalReadingSession` + persister |
| `RunPendingPdfLocalReading` | — | Core extract | **Done** — same session boundary |
| `ParseApprovedPdfWithIsolatedParser` | — | Core extract | **Done** — descriptor access + borrowed parser ports |
| `PersistValidatedPdfLocalReading` | — | Core extract | **Done** — moved to `data/pdfbox/isolation/` |
| `ClearMemoraDerivedData` | — | App privacy | **Done** — `UserConfirmedDerivedDataClearer` |
| `DownloadOnDeviceEmbeddingModel` | — | App / capability | **Done** — `OnDeviceEmbeddingModelDownloader` + store |
| `IndexOneNotePages` | `OneNotePagesDiscoverySource` | **App-only** | Move to `application/notes` App bucket; no Core |
| `OpenPersistedNotePageInOneNote` | Graph gateway | **App-only** | Stay App |
| `RunPendingOneNotePageExtract` | Room, OneNote reader | **App-only** | Stay App |

**Counts (2026-08-30):** 3 allowlisted files (OneNote App bucket only); 12 Core-clean application files.

**Phase 1 Core exit (slice 4):** All non–OneNote `application` use cases route through domain ports. Remaining allowlist is intentional App-only connector debt.

---

### Phase 2 — Gradle modules (still one git repo)

**Goal:** Compiler-enforced boundaries inside `MemoraApp/`.

| Module | Type | Depends on | Contains |
|---|---|---|---|
| `:core-domain` | Kotlin JVM / Android library (min) | nothing Memora | `domain/**` pure |
| `:core-application` | Android library | `:core-domain` | `application/memory`, `application/discovery`, intelligence use cases (ported) |
| `:core-data` | Android library | `:core-domain` | Room, DAOs, platform adapters for Core |
| `:app` | application | `:core-application`, `:core-data` | `ui`, `work`, Hilt modules, product glue |

**Sequence:**

1. Create empty `:core-domain`; move `domain/`; fix imports; all tests green.
2. Create `:core-application`; move memory + intelligence application layer.
3. Create `:core-data`; move `data/local` memory/asset/evidence paths first.
4. Leave `data/mediastore`, `data/notes`, `data/saf` in `:app` or `:app-data` until ports are clean.
5. Update Hilt modules to bind ports in `:app` or `:core-data`.

**Authorization:** ADR or change-control + architecture review (module graph).  
**Exit:** `./gradlew :app:assembleDebug` and unit tests pass; dependency graph matches §4.

---

### Phase 3 — Publish Core as library (private Maven first)

**Goal:** App consumes Core as a versioned artifact (practice run before public).

1. Choose coordinates (e.g. `com.unfynd.core:domain`, `com.unfynd.core:application-android`).
2. Publish to **private** Maven (GitHub Packages or local) from CI on tag.
3. Point `:app` at published AAR/JAR instead of project dependencies (optional toggle).
4. Document version policy (semver on Core API surface).

**Authorization:** ADR (artifact IDs, versioning, license still private).  
**Exit:** Clean tag build; App runs against published Core 1.x.

---

### Phase 4 — Public Core source (Class B subset)

**Goal:** Expand `unfynd-core` GitHub from docs-only to **runnable library source**.

**Prerequisites (gates from strategy §23):**

- [ ] Architecture: module graph stable; Recall convergence plan documented
- [ ] IP / provenance: third-party deps cleared for redistribution
- [ ] License: Apache-2.0 vs source-available decision per bucket
- [ ] Security: disclosure process; no secrets in history
- [ ] Quality: Core unit tests run in public CI; documented Android API level
- [ ] Commercial: what remains proprietary (AI packs, enterprise SDK)

**Mechanics:**

1. ADR: exact paths and license per module.
2. Subtree or copy `:core-domain` + `:core-application` + selected `:core-data` to public repo.
3. Public CI: `./gradlew :core-domain:test` (and android tests if needed).
4. Keep sync script (same pattern as Class A pack: never push private monorepo whole).
5. Class A `SPEC.md` remains; add `BUILDING.md` when code lands.

**Authorization:** **New ADR required** (Class B / broader open). ADR-047 does not cover this.  
**Exit:** External clone builds Core; App still private.

---

### Phase 5 — Multi-platform App repos (future)

Windows / iOS / Mac App surfaces consume **same Core** (Kotlin Multiplatform or
native bindings — **not decided here**). Android `MemoraApp/` becomes one
consumer. Requires separate product ADRs per platform.

---

## 7. Coupling hotspots (fix in Phase 1)

These `application` → `data` imports block portable Core today:

| Area | Example files | Remediation |
|---|---|---|
| PDF viewing / local reading | `OpenPersistedPdfForViewing`, `RunPdfLocalReadingStatusCheck` | `domain` port `PdfOriginalOpener`; impl in `data/saf` |
| Screenshot / photo open | `OpenPersistedScreenshotForViewing`, `OpenPersistedPhotoForViewing` | `MediaStoreContentOpener` port |
| OneNote | `OpenPersistedNotePageInOneNote`, `IndexOneNotePages` | Keep in **App module**; not Core |
| MSAL / Graph | via `application/notes` | App-only integration |

Rule: **Core orchestrates memory/recall/indexing**; **App orchestrates product
connectors and viewers**.

---

## 8. Recall / MIG alignment

Do **not** split modules in the middle of legacy recall chaos.

| Milestone | Relation to separation |
|---|---|
| MIG-07B (authorized separately) | Fold L7/L8 into `CanonicalRecall` **before** publishing Core recall API as stable |
| `RECALL_CONVERGENCE_DONE` | Core public API should not freeze until convergence checklist met |
| MIG-05 claim B (non-PDF evidence indexer) | Can stay private; does not block domain module |

**Public Core API stability:** treat `CanonicalRecall` + result types as semver
surface once Phase 3 begins.

---

## 9. Verification checklist (each phase)

| Check | Command / artifact |
|---|---|
| Unit tests | `./gradlew :app:testDebugUnitTest` (and module-scoped as split) |
| Assemble | `./gradlew :app:assembleDebug` |
| Legacy recall guard | `scripts/check-legacy-recall-surface.ps1` |
| Dependency rule | Custom script: no `core-domain` → android UI; no `ui` → `data` direct |
| Secret scan | Before any public push of Core source |
| Class A sync | Pack-only subtree to `unfynd-core` (docs until Phase 4) |

---

## 10. What we are not doing in this plan

- Splitting git repos immediately
- Claiming “UNFYND Core is fully open source” (runnable) before Phase 4 ADR
- Opening AI Pack weights, App UI, or private monorepo wholesale
- Renaming `com.memora.app`, `memora.db`, or GitHub `Memora` (ADR-040 deferred)
- iOS / Windows / Mac implementation

---

## 11. Recommended next action (engineering lead)

**Start Phase 1.1:** generate the `application` → `data` import inventory and
fix the highest-traffic path (memory/recall already clean; defer OneNote to App-only
bucket).

When Phase 1 exit is met, open change-control for **Phase 2 Gradle modules**.

---

## 12. References

- `docs/OPEN_SOURCE_COMMERCIAL_STRATEGY.md` §10, §18, §23–24
- `public/unfynd-core/ROADMAP-OPEN.md`
- `docs/DECISIONS.md` ADR-047, ADR-048, ADR-049
- `docs/CHANGE_CONTROL_CANONICAL_RECALL_THIN_FACADE.md`
- `docs/RECALL_ENFORCEMENT_INDEX.md`
- `docs/UNFYND_VISION_ALIGNMENT.md` (MVP vs long-term multimodal)
