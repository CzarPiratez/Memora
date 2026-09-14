# Change Control — Notes connector (OneNote-class)

**Date opened:** 2026-07-31  
**Status:** N0–N7 accepted. OneNote-class Notes connector MVP path closed for
this change-control (discover → extract → keyword recall → Build-memories →
open original). Semantic ranking remains deferred to Local-AI track.  
**Requirements:** P-03, P-08, P-15, P-19; A-01, A-02, A-06; product contract Notes
row; ADR-001, ADR-003, ADR-004.  
**Decision guardrails:** ADR-003 Choice 1 — one narrowly scoped, read-only
provider connector (first target: Microsoft OneNote). Not a Memora account, not
cloud sync of Memora data, not write-back into OneNote, not Share-as-indexing, not
arbitrary note-app scanning.

## Pre-work record

- **Sources read:** Product registry, Local AI Technical Specification (offline +
  provider access), governance, product contract (Notes + platform constraint),
  architecture, decisions (ADR-003/004), roadmap, traceability (P-08/P-19),
  CONTINUE, CHANGELOG ADR-003 entry, Asset Memory persistence change-control.
- **Current-code evidence inspected:** `AssetType.NOTE` already exists on
  `Asset`; `RoomAssetMemoryFactSource` returns `emptyList()` for `NOTE`; no
  OneNote/MSAL/Graph adapter, no Notes setup UI, no note extraction Room table.
- **Open ADRs / limitations:** ADR-003 decision is accepted; this record **opens**
  the implementation change-control it required. SAF note-folder fallback
  (ADR-003 Choice 2) stays deferred and needs its own change-control if chosen.
- **Privacy / network / offline / retention:**
  - **Original notes** stay owned by Microsoft / OneNote; Memora stores only
    normalized Asset/extraction/Memory rows the user authorized, in the encrypted
    on-device database.
  - **Network is required** for Microsoft sign-in and Graph discovery/extract.
    That is source access, not Memora cloud sync and not Local-AI dependency
    (Local AI Spec §10).
  - After note text is saved locally, **keyword recall and Explain over saved
    facts must work offline**. Refresh / new pages need network again.
  - **No** client secrets, refresh tokens, or long-lived access tokens in Git or
    the APK. Tokens live only in platform-secure storage after user consent.
  - Disconnect / revoke must stop further Graph calls and leave honest copy about
    already-saved local facts (Clear Memora index remains the wipe for Memora-owned
    rows).
- **Smallest safe change for this open record:** documentation only — phase plan,
  honesty rules, acceptance gates, and the Azure app-registration external
  dependency. No MSAL dependency and no Graph calls yet.
- **Acceptance for opening this slice:** ADR-003 rule satisfied ("Notes
  change-control slice is opened"); CONTINUE points here; P-08/P-19 no longer say
  "blocked with no path."

## Honest product framing (must stay true in UI and docs)

- Memora indexes **OneNote through an approved connector**, not “all notes on the
  phone.”
- Connecting uses the user’s **Microsoft account for OneNote only** — it is not a
  Memora login and does not sync Memora’s index to Microsoft.
- Until a phase ships live discovery, UI must not imply notes are already
  searchable.

## Phased delivery (approve each phase before code)

| Phase | Goal | Ships code? | Hard gate |
|-------|------|-------------|-----------|
| **N0** (this) | Open change-control + phase plan | Docs only | User accepts plan |
| **N1** | Honesty + setup entry | Yes — copy/navigation only | No fake “connected”; explains network + Microsoft auth before any SDK |
| **N2** | Auth + secure token vault | Yes — MSAL (or documented equal), Encrypted storage, disconnect | Azure app registration + Android redirect URI; unit tests for token absence in APK/resources; revoke path |
| **N3** | Discovery → `Asset` NOTE placeholders | Yes — Graph notebooks/pages → stable identity | Bounded, user-started drain; read-only scopes; no write APIs |
| **N4** | Extract note text + metadata | Yes — Room persistence parallel to PDF/OCR honesty | Deterministic facts only; empty text is a completed extract |
| **N5** | Interim keyword search + Memory assembly | Yes — Find saved note text; optional Build-memories inclusion | Same honesty as PDF/OCR keyword path; no semantic ranking claim |

**Recommended next code slice after N0 approval:** **N1 only**.

## External dependency (cannot hide)

Shipping N2+ requires a **Microsoft identity platform app registration**
(public native/Android client): application (client) ID, Android package name +
signature hash redirect, and least-privilege Graph scopes (read-only notes).

- Client ID may be packaged as a non-secret public identifier once registered.
- **No** client secret for this public client model.
- Registration is owned outside the repo (developer portal). Without it, N2 cannot
  be verified on a real account.

If registration or privacy constraints block OneNote delivery, stop and reopen
ADR-003 Choice 2 (SAF note files) under a separate change-control — do not fake
progress with Share-as-primary.

## Architecture boundary (target)

```
UI (setup / connect / disconnect)
  → application use cases
    → domain NOTE Asset + extraction contracts
      → platform OneNote adapter (Graph) + secure token store
      → Room persistence
```

Dependencies flow inward. Composables never call Graph. Workers/use cases own
bounded drains. Understanding/AI Pack remain out of scope for these phases.

## Explicit non-goals (all phases until amended)

- Memora user accounts or Memora cloud sync
- Write/update/delete in OneNote
- Indexing Google Keep, Samsung Notes, or any app without its own approved ADR
- Share sheet as the primary Notes indexing model
- Semantic Memory ranking, embeddings, or AI Pack for note understanding
- Storing provider secrets in Git / BuildConfig secrets committed to the repo

## Verification plan (by phase)

- **N0:** Docs review + this acceptance record.
- **N1:** Copy review; unit tests for strings/state if introduced; emulator smoke
  that Notes entry explains connector + network without claiming indexed notes.
- **N2+:** Separate change-control amendment or continuation section with MSAL
  dependency review, scope list, revoke tests, and emulator/device auth smoke on a
  throwaway Microsoft test account.

## Acceptance record — N0

- **Delivered:** This file; CONTINUE updated to open Notes N0 and point next code
  at N1; traceability P-08/P-19 path language updated.
- **Not delivered:** MSAL, Graph, note Room tables, Connect OneNote UI beyond any
  honesty-only entry approved in N1.
- **Git:** `597de86`.

## Acceptance record — N1 (accepted 2026-08-01)

- **Delivered:** Welcome → **About Notes indexing** opens honesty screen
  (`NotesConnectorHonestyCopy` / `NotesConnectorHonestyScreen`). Explains OneNote
  connector, Microsoft auth ≠ Memora account, network for source access, and that
  notes are not indexed yet. No Connect button, no MSAL/Graph dependency.
- **Automated:** `NotesConnectorHonestyCopyTest` passed.
- **Emulator/manual:** User confirmed Notes indexing copy and Back returns to
  welcome (2026-08-01).
- **Git:** `67beb21`.

## N2 continuation — Auth + secure token vault

Split so the Azure external gate does not block vault honesty work.

| Sub-phase | Goal | Ships | Hard gate |
|-----------|------|-------|-----------|
| **N2a** | Secure session vault + registration-config gate + honest status UI | Yes — Keystore-backed vault, BuildConfig/local.properties client ID plumbing, Disconnect clears vault; **no** MSAL, **no** INTERNET, **no** fake Connected | Unit tests: vault clear; no client_secret in resources; status copy never claims notes indexed |
| **N2b** | MSAL sign-in + token into vault + live Disconnect/revoke | Yes — MSAL dep, INTERNET, Connect | Azure Android registration (`docs/NOTES_ONENOTE_AZURE_APP_REGISTRATION.md`); dependency review pinned; throwaway-account emulator smoke |

### N2 scopes (delegated Graph only)

- `Notes.Read` — OneNote read (used from N3+)
- `User.Read` — optional minimal signed-in identity for account label
- `offline_access` — provided by MSAL by default (do **not** also pass it in the
  Android scope list; that can trigger DeclinedScope)

No write scopes. No app-only / client secret.

### N2a architecture

```
UI (Notes status / Disconnect)
  → presentation copy + ViewModel/use of vault snapshot
    → NotesProviderTokenVault (domain port)
      → Keystore-backed encrypted session store (platform)
OneNoteAuthConfiguration ← BuildConfig from local.properties (public client ID)
```

### Acceptance record — N2a (accepted 2026-08-01)

- **Delivered:** Registration runbook; Keystore session vault; BuildConfig /
  local.properties client ID + signature hash; Notes status for registration
  required / disconnected; Disconnect + clear-index vault clear. No MSAL yet in
  N2a itself.
- **Automated:** Copy/config/vault/secret-absence unit tests passed.
- **Emulator:** Registration-present status confirmed after local.properties
  wiring (2026-08-01).
- **Git:** Include with N2b or prior N2a checkpoint as applicable.

### Acceptance record — N2b (accepted 2026-08-01)

- **Delivered:** MSAL 8.4.1; INTERNET for Microsoft source access; Connect uses
  single-account `signIn` (with mismatch recovery); Disconnect clears MSAL +
  Keystore vault; honest connected copy without indexing claims.
- **Not delivered:** Graph discovery, note extract, note keyword search (N3–N5).
- **Automated:** Notes honesty/config/vault/secret-absence unit tests passed.
- **Emulator/manual:** User connected as `mir.m@outlook.com`, then Disconnect
  returned to not-connected (2026-08-01). Google Custom Tab intermediary is
  expected; Microsoft account completes the flow.
- **Git:** Record in the N2b acceptance commit.

## N3 continuation — Discovery → NOTE placeholders

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Bounded Graph page list → `AssetType.NOTE` placeholders | Yes — Graph GET only; user-started Discover; stable page id identity; **per-section** pages walk (avoids global `/pages` error 20266) | Connected = vaulted token; no write APIs; no HTML extract; UI must not claim searchable notes |

### Acceptance record — N3 (accepted 2026-08-01)

- **Delivered:** `OneNotePagesDiscoverySource` (sections → pages), Graph gateway,
  `IndexOneNotePages`, Notes **Discover / Discover more**, `ensureSession` +
  vault-only Connected honesty, MSAL `signInAgain` when account already present,
  scopes without duplicate `offline_access`.
- **Not delivered:** Page HTML extract (N4), note keyword search (N5).
- **Automated:** `OneNotePagesDiscoverySourceTest`, `IndexOneNotePagesTest`,
  Graph parse tests passed.
- **Emulator smoke (user):** Connect `mir.m@outlook.com` → Discover saved 25
  placeholders → Discover more → 28 total; honesty “not searchable yet”
  (**accepted** 2026-08-01).

## N4 continuation — Extract note page text

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Graph GET page HTML → plain text in Room | Yes — `note_page_extractions` (Room v9); user-started extract drain; one page per worker step | Connected vaulted token; empty text = completed extract; UI must not claim searchable notes (N5) |

### N4 architecture

```
UI Extract OneNote page text
  → WorkManager (unique drain)
    → RunPendingOneNotePageExtract
      → findNext NOTE pending extract
      → Graph GET contentUrl (HTML) → plain text
      → Room note_page_extractions
```

### Acceptance record — N4 (accepted 2026-08-01)

- **Delivered:** Room `note_page_extractions` (v9); pending NOTE drain via
  WorkManager; Graph HTML → plain text; Notes **Extract OneNote page text** +
  counts; honesty “not searchable yet” (superseded by N5 Find saved note text).
- **Not delivered at N4 close:** Keyword Find saved note text (N5); Memory
  assembly from notes.
- **Automated:** HTML plain-text + extract decision-mapper + Notes copy tests
  passed; Room schema `9.json` exported.
- **Emulator smoke (user):** Connected `mir.m@outlook.com`; 29 placeholders →
  Extract → **29 text extracts**; feedback “Saved note text for 29 page(s)”
  (**accepted** 2026-08-01 via screenshot).

## N5 continuation — Find saved note text (keyword)

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Keyword search over saved note page text | Yes — Welcome **Find saved note text**; Room LIKE on `note_page_extractions`; Why evidence; offline after extract | Same honesty as PDF/OCR keyword path; no Graph during search; no semantic ranking; Memory assembly deferred |

### Acceptance record — N5 (**accepted** 2026-08-02)

- **Delivered (this slice):** `SearchPersistedNotePageText`, readiness, ViewModel/Screen,
  Welcome entry, Why this result?, Notes honesty points to Find saved note text.
- **Not delivered at N5 close:** Open original in OneNote/browser; Build-memories
  NOTE facts; semantic ranking.
- **Automated:** Note keyword search copy/ViewModel + Notes honesty copy unit tests
  passed; `:app:installDebug` on emulator.
- **Emulator smoke (user):** Welcome → Find saved note text; readiness **28 note
  pages** ready; query `pass` → 1 match with title + excerpt highlight; Why this
  result? showed keyword evidence and honesty line (**accepted** 2026-08-02 via
  screenshot).

## N6 continuation — Build-memories NOTE facts

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Asset Memory from saved note page text | Yes — `NOTE_TEXT` + optional `note:title` from Room `note_page_extractions`; pending drain includes NOTES; setup copy updated | Offline assembly only; blank extracts skipped; no Graph; no semantic ranking |

### Acceptance record — N6 (**accepted** 2026-08-02)

- **Delivered (this slice):** `AssetMemoryFact` allows `NOTE_TEXT`;
  `RoomAssetMemoryFactSource` + `AssetMemoryFactDao` pending/load for notes;
  Saved fact memories copy mentions OneNote page text.
- **Not delivered at N6 close:** Open original in OneNote/browser (N7); semantic
  ranking.
- **Automated:** NOTE assembly + Asset Memory setup copy unit tests passed;
  `:app:installDebug` on emulator.
- **Emulator smoke (user):** Welcome Saved fact memories body includes OneNote
  page text; **28** current evidence-backed Asset Memories saved (**accepted**
  2026-08-02 via screenshot).

## N7 continuation — Open original in OneNote/browser

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Open hit’s original OneNote page externally | Yes — on-demand Graph `links` (`oneNoteWebUrl` then client URL); ACTION_VIEW; vaulted `ensureSession` | Never ACTION_VIEW `contentUrl`/location; search stays offline; honesty that Open may need network |

### Acceptance record — N7 (**accepted** 2026-08-02)

- **Delivered (this slice):** Graph `getPageLinks`; `OpenPersistedNotePageInOneNote`;
  Find saved note text → **Open original note** via ACTION_VIEW; vaulted
  `ensureSession`; honest Opening / SourceUnavailable / CouldNotOpen feedback.
  Never ACTION_VIEW `contentUrl` / Asset.location.
- **Auth reliability (same slice):** Reuse one MSAL PCA instance; silent refresh
  when vault token is expired/near expiry; Open retries once after Graph 401;
  interactive Connect uses `authorization_user_agent: WEBVIEW` so sign-in stays
  in Memora (avoids BrowserTabActivity “no interactive call in progress” toast).
- **Open target:** Prefer OneNote app deep link (`oneNoteClientUrl`) when the
  device can resolve it; otherwise browser web URL. Open work runs on IO with a
  timeout to avoid ANR when Graph/MSAL is slow.
- **Not delivered:** In-app note preview; persisting open URLs into Room; semantic
  ranking.
- **Automated:** Open use case, links parse, VM/copy unit tests passed;
  `:app:installDebug` on emulator.
- **Emulator smoke (user):** Open original note opened the page in browser
  (`onedrive.live.com`) — expected without OneNote app on emulator; real phones
  with OneNote prefer the app deep link (**accepted** 2026-08-02).

### Defect N7-D1 — the app deep link never ran on a real phone (2026-09-14)

The N7 record above claimed real phones prefer the OneNote app. They did not.
The founder's phone has OneNote installed and every Open original note still
landed in the browser.

- **Root cause.** `AndroidExternalUrlLauncher` gated the `onenote:` deep link
  behind `PackageManager.resolveActivity`. Android 11+ package-visibility
  filtering returns null for another app's custom scheme unless the caller
  declares it in a manifest `<queries>` element, and this app declares none.
  The gate therefore failed on every modern device *whether or not OneNote was
  installed*, the web URL was tried next, it succeeded, and the "last resort"
  client attempt below it was unreachable. A second, smaller fault: the intent
  added `CATEGORY_BROWSABLE` to the app scheme, which a deep-link activity has
  no reason to declare.
- **Fix.** `OneNoteOpenTargetPolicy` orders the targets — client link first,
  then web — and the launcher *attempts* them in order. Package visibility
  does not restrict `startActivity` for an implicit intent, so an installed
  OneNote now opens and a device without it throws, is caught, and falls
  through to the browser exactly as before. `BROWSABLE` is added only for
  `http`/`https`.
- **Unchanged.** Still `links`-only (never `contentUrl` / `Asset.location`),
  still vaulted `ensureSession`, still offline search, same honest
  Opening / SourceUnavailable / CouldNotOpen feedback.
- **Automated:** `OneNoteOpenTargetPolicyTest` (order, blank/missing links,
  duplicate pair, BROWSABLE rule). Full `:app:testDebugUnitTest`
  **822 tests, 0 failures** (2026-09-14).
- **Device gate (open):** founder taps Open original note on the phone with
  OneNote installed and the OneNote app opens the page. A JVM test cannot
  prove this; the emulator has no OneNote and will still show the browser,
  which remains correct behavior.
- **Not done here:** no manifest `<queries>` entry. Nothing needs to *query*
  OneNote now. Declaring one becomes necessary only if UNFYND wants to say
  "OneNote is not installed" before the tap.
