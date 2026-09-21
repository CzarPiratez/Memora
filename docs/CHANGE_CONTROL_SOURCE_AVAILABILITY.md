# Change control: Source availability (Open-confirmed honesty)

**Date:** 2026-09-20  
**Type:** Orthogonal Asset observation + Find Open presentation  
**Closes:** ADR-056 slice 1 — persist last confirmed Open reachability; standing
honesty on Canonical Recall cards  
**Does not authorize:** ranking changes, ADR-055 work, Forget / CR-08, discovery
absence tombstones, search-time original probes, Memory integrity rewrites,
AVAILABLE, a new Find path, or Live/Dual growth

Slice 1 (standing Open honesty) is delivered. Slice 2 (user-initiated Hide
from Find) is delivered. Slice 3 (note Open-class, local only) is delivered
below. Discovery absence and CR-08 remain later.

## Pre-work record

- **Requirement IDs:** I16 / R1 (Find succeeded, Open failed — keep Memory and
  Why; tell the truth about Open). P-11 / LOCAL_AI §3 (search does not reopen
  originals *to answer a query*). P-01 / P-15 (originals stay where they are;
  UNFYND does not silently delete derived knowledge). E-05 (source gone ≠
  Memory gone).
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE pre-work gate,
  CONTINUE (ADR-055 is a parallel track — this record does not rewrite it),
  PRODUCT_SOURCE_REGISTRY, LOCAL_AI_TECHNICAL_SPEC, RECALL_ENFORCEMENT_INDEX,
  LEGACY_RECALL_SURFACE (Live/Dual **N = 0**), HUMAN_RECALL_ASK_MODEL R1,
  MEANING_FIND_PRODUCT_SCENARIO_BAR I16, CHANGE_CONTROL_TEMPLATE.
- **Current-code evidence inspected:** Find Open paths collapsed
  AccessRevoked into SourceUnavailable; Memory READY is the searchable
  integrity; discovery is additive-only; Open feedback was ephemeral, so a
  deleted file looked live on the next search.
- **Open ADRs / platform:** ADR-056 accepted in this slice. Open's current
  outcomes do not distinguish delete vs move vs revoked grant — copy must not
  claim a single cause. Discovery absence is a later slice: incomplete pages
  must not tombstone.
- **Privacy:** Observations are UNFYND-owned, keyed by source identity, no
  original bytes, no cloud. Search still ranks stored Memory only.
- **Smallest safe change:** new `source_availability_observations` table
  (Room 17→18); record confirmed Open success/unreachable; join onto already
  ranked hits; standing card copy + Try Open. Memory integrity unchanged.
- **Acceptance criteria:**
  - [x] Memory stays READY after Open-unreachable
  - [x] Search does not probe originals
  - [x] Confirmed Open-unreachable persists and shows on the card before the next tap
  - [x] Try Open retries; successful Open resurrects REACHABLE
  - [x] Retryable CouldNotOpen is not recorded as unreachable
  - [x] List thumbnail reopen that cannot reach a PDF updates the card without an Open tap
  - [x] List thumbnail reopen that cannot reach a photo or screenshot (missing
        asset, missing descriptor, or every URI candidate unreachable) updates
        the card without an Open tap
  - [x] Photo/screenshot permission-off and decode flakes stay UNAVAILABLE —
        not recorded as gone
  - [x] Copy keeps Memory/Why; does not claim a single cause or “unavailable.”
  - [x] Standing unreachable copy is list-density (not an error essay); Try Open
        is secondary; filename does not hyphenate mid-extension
  - [x] Keyword + meaning Find cards share the same honesty control
  - [x] No ranking / ADR-055 files in this change
  - [x] Device 2026-09-21: deleted indexed PDFs still Find; standing honesty
        before Open; compact copy + outlined Try Open; a live PDF on the same
        page still showed a thumbnail. Photo restore/resurrection not separately
        filmed.
- **Holistic scenarios (before implement):**
  - User: deleted a photo that still matches a cue — the card is honest before tap
  - User: file restored to the same identity — Try Open resurrects
  - User: renderer flake (CouldNotOpen) — must not permanently mark unreachable
  - User: TalkBack hears standing copy, then Try Open
  - Technical: ranking never reads availability
  - Technical: other agent on ADR-055 ranking — this slice stays off that stack
- **Alternatives considered:**
  - Hide the hit / set Memory SOURCE_UNAVAILABLE — rejected (I16 anti-case;
    collapses revoke vs delete; fights P-01).
  - Probe originals during Find — rejected (P-11).
  - Discovery-absence tombstone in this slice — rejected (incomplete pages).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Open honesty on already-ranked Canonical Recall hits
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (Live/Dual N = 0)
TARGET PATH: Canonical Recall ranks Memory; SourceAvailabilityStore joins last
  confirmed Open observation by Asset identity; presentation only
WHY THIS CHANGE CONVERGES: not a Find path — candidate generation and ranking
  unchanged; availability never produces a search hit
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: ephemeral Open-only toast/card state
  that forgot the miss on the next search
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — UI still cannot produce a search hit without
  Canonical Recall
```

LOCAL_AI §3 / P-11: query answering still uses stored evidence. List
thumbnail reopen is Open-class observation for recognition and honesty,
never a ranking input.

## Delivery record

- **Files/layers:** domain `SourceAvailability*`; Room 18
  `source_availability_observations`; application `RecordOpenSourceAvailability`
  / `LoadSourceAvailability`; `ImageThumbnailLoad` Unreachable vs CouldNotDecode
  for photos/screenshots; Find ViewModels join after search and persist
  confirmed Open and Open-class list thumbnails; `FindOpenOriginalButton`
  standing copy + Try Open.
- **Automated verification:** `SourceAvailabilityObservationTest`,
  `RecordOpenSourceAvailabilityTest`, `FindOpenTargetAvailabilityTest`,
  `FindSourceAvailabilityCopyTest`, Meaning ViewModel persist/resurrect +
  CouldNotOpen non-record. `LoadFindResultThumbnailTest` covers missing
  photo, unreachable URI, screenshot decode flake, later URI candidate,
  empty candidates, and PDF gone vs flake. `FindThumbnailPolicyTest`
  `imageMissGlyph`. Focused `:app:testDebugUnitTest` **BUILD SUCCESSFUL**
  (2026-09-20). Migration 17→18 asserted in `MemoraDatabaseMigrationTest`
  (instrumentation; not run this pass).
- **Emulator/manual:** founder device **pass** 2026-09-21 — deleted PDFs
  still Findable with standing honesty; slice 1c card craft accepted
  (compact copy, type glyph, filename wrap, outlined Try Open). A live
  PDF on the same page still previewed.
- **Failure/recovery:** unknown = no row; corrupt enum rows ignored on read;
  CouldNotOpen stays retryable; successful Open overwrites UNREACHABLE.
- **Known limitation:** notes had no local thumbnail reopen in this slice
  (closed in slice 3). Discovery absence and CR-08 remain later. Open still
  collapses delete/move/revoke.
- **Git commit:** pending (local checkpoint not created this pass).

## Slice 2 — user-initiated Hide from Find

**Closes:** ADR-056 slice 2 — reversible recall-visibility preference  
**Does not authorize:** auto-hide, ranking, ADR-055, CR-08 erase, discovery
absence, search-time probes, Memory integrity changes, a new Find path

### Pre-work record

- **Requirement IDs:** I16 / R1 (keep Memory and Why; silent hide is the
  anti-case). Hide here is **user-initiated** and reversible — not silent
  auto-hide, not CR-08. P-01 / P-15 originals stay. E-05 source gone ≠
  Memory gone.
- **Current-code evidence inspected:** standing honesty (slice 1) left
  unreachable cards in the list with no door to put them away; CR-08 is
  cascade erase with proof — a different product.
- **Smallest safe change:** Room 18→19 `find_hidden_identities`; presentation
  filter on already-ranked Canonical Recall hits; Hide only on standing
  unreachable; Show again restores the identity to that ranked list without
  a new query.
- **Acceptance criteria:**
  - [x] Memory stays READY after Hide
  - [x] Ranking and candidate generation never read the hide store
  - [x] Hide is user-initiated; unreachable cards are not auto-hidden
  - [x] Show again is durable and restores the card on keyword and meaning Find
  - [x] All-hidden is not NoMatches — copy says Memory is still on this phone
  - [x] Hide is not CR-08 and does not delete originals or Memories
  - [x] User-confirmed derived-data clear rebuilds the UNFYND database, so
        hide rows go with it; refresh the panel after clear
  - [x] Restore list is this-search only; clearing the cue does not turn
        Find home into a hidden-files notice
  - [x] Hide offers a snackbar Show again; footer is list-density
  - [x] No ranking / ADR-055 files in this change
  - [x] Device 2026-09-21: Hide an unreachable PDF; it leaves the list;
        snackbar Show again; clearing the cue is a search home, not a
        hidden-files notice
- **Holistic scenarios:**
  - User: deleted file still Finds — Hide puts it away; Memory/Why remain
  - User: Show again on any Find surface restores the same identity
  - User: hide on PDF Find applies to Meaning (one preference)
  - User: clearing the cue returns a search home, not a hidden-files list
  - User: Hide shows a snackbar Show again without a standing essay
  - User: TalkBack hears Hide from Find only after standing unreachable copy
  - Technical: ranked hits stay in ViewModel state; filter is presentation
  - Technical: other agent on ADR-055 ranking — this slice stays off that stack
- **Alternatives considered:**
  - Auto-hide on Open-unreachable — rejected (I16 silent-hide anti-case).
  - CR-08 / Memory delete — rejected (wrong product; not reversible).
  - Per-surface hide — rejected (one identity, one preference).

### Architectural convergence

```
ARCHITECTURAL BOUNDARY: presentation filter on already-ranked Canonical Recall hits
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (Live/Dual N = 0)
TARGET PATH: Canonical Recall ranks Memory; FindHiddenStore omits hidden
  identities from the list only; Show again restores the same ranked hits
WHY THIS CHANGE CONVERGES: not a Find path — candidate generation and ranking
  unchanged; hide never produces or scores a search hit
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none — adds a user preference beside
  standing honesty
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — UI still cannot produce a search hit without
  Canonical Recall
```

### Delivery record

- **Files/layers:** domain `FindHidden*`; Room 19 `find_hidden_identities`;
  application `HideFromFind` / `ShowAgainOnFind` / `LoadFindHidden` /
  `FindHiddenPolicy`; `FindHiddenViewModel` shared across Find surfaces;
  Hide control on standing-unreachable `FindOpenOriginalButton`; Show-again
  only for identities this search ranked; snackbar undo after Hide; idle
  Find does not list hidden files.
- **Automated verification:** `FindHiddenItemTest`, `FindHiddenPolicyTest`,
  `HideFromFindTest`, `FindHiddenCopyTest`. Focused `:app:testDebugUnitTest`
  **BUILD SUCCESSFUL** (2026-09-21). Migration 18→19 asserted in
  `MemoraDatabaseMigrationTest` (instrumentation; not run this pass).
- **Emulator/manual:** founder device **pass** 2026-09-21 — Hide from Find
  on unreachable PDFs; snackbar undo; idle/cleared cue is a search home,
  not a hidden-files list.
- **Failure/recovery:** corrupt hide rows ignored on read; blank labels
  rejected; derived-data clear wipes hide with the UNFYND database.
- **Known limitation:** Hide is offered on standing unreachable only (the
  deleted-file door). Slice 3 gives notes that standing state without an
  Open tap for missing Asset / disconnect-without-URL. A stored OneNote
  URL that 404s still needs Try Open. Discovery absence and CR-08 remain
  later.
- **Git commit:** pending (do not commit unless asked).

## Slice 3 — note Open-class (local, not a search probe)

**Closes:** ADR-056 slice 3 — standing honesty for notes without a list thumbnail  
**Does not authorize:** Graph/session refresh at search, ranking, ADR-055, CR-08,
discovery-absence tombstones, Memory integrity changes, a new Find path

### Pre-work record

- **Requirement IDs:** I16 / R1; P-11 / LOCAL_AI §3 (search does not reopen
  originals *to answer a query*). Notes have no Open-class list thumbnail.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE pre-work,
  CONTINUE (ADR-055 parallel), PRODUCT_SOURCE_REGISTRY, LOCAL_AI §3,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (N = 0), HUMAN_RECALL
  R1, MEANING_FIND I16, CHANGE_CONTROL_TEMPLATE, ADR-056.
- **Current-code evidence inspected:** PDF/photo/screenshot learn gone
  originals from list thumbnail reopen. Notes always render a NOTE glyph.
  Open prefers a local `note_page_open_targets` row and only then Graph.
  `ensureSession` can hit MSAL — it must not run for Find.
  `tokenVault.readSession()` is local Keystore only.
- **Privacy:** Grant presence is a local vault read. Access tokens are
  never used as a query input. No Graph. No original bytes.
- **Smallest safe change:** after stored availability, for note identities
  still UNKNOWN, join local Asset + open-target + vaulted grant. Persist
  only a missing Asset. No Graph.
- **Acceptance criteria:**
  - [x] Search does not call Graph or `ensureSession`
  - [x] Stored Open observation still wins
  - [x] Missing note Asset → standing unreachable and persisted
  - [x] Stored OneNote URL without a grant stays live (local handoff)
  - [x] No URL and no vaulted grant → standing unreachable, not persisted
        (Connect can resurrect without a tap)
  - [x] Photos/PDFs are not reclassified by this join
  - [x] Try Open still retries; successful Open still resurrects
  - [x] Ranking / ADR-055 files untouched except Meaning Find availability join
  - [x] Standing note copy is list-density; OneNote handoff hint is not shown
        on an already-unreachable card
  - [ ] Device: disconnected OneNote, a note without a stored open URL shows
        standing honesty before Open; a note with a stored URL still offers Open
- **Holistic scenarios:**
  - User: disconnected OneNote — notes without URLs tell the truth before tap
  - User: Connect again — those cards are live again without Hide/Show
  - User: note deleted from OneNote but URL still stored — still needs Open/Try
    Open (not discovery absence)
  - Technical: ranking never reads this join
- **Alternatives considered:**
  - Graph `links` at search — rejected (P-11; 6–7s).
  - Persist disconnect as UNREACHABLE — rejected (grant-like; Connect must
    resurrect).
  - Discovery-absence tombstone — rejected (incomplete pages).

### Architectural convergence

```
ARCHITECTURAL BOUNDARY: presentation join on already-ranked Canonical Recall hits
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (Live/Dual N = 0)
TARGET PATH: Canonical Recall ranks Memory; LoadFindOpenAvailability joins stored
  Open observations then local note Open-class; never a candidate generator
WHY THIS CHANGE CONVERGES: not a Find path — notes learn standing honesty the
  way files learned it from list thumbnails, from UNFYND-owned rows only
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: note cards that looked live until Open
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

### Delivery record

- **Files/layers:** `NoteOpenClassPolicy`; `LoadNoteOpenClassAvailability`;
  `LoadFindOpenAvailability` used by meaning + note keyword Find.
- **Automated verification:** `NoteOpenClassPolicyTest`,
  `LoadNoteOpenClassAvailabilityTest`, `FindSourceAvailabilityCopyTest`,
  `MeaningSearchViewModelTest`, `NotePageKeywordSearchViewModelTest`.
  Focused `:app:testDebugUnitTest` **BUILD SUCCESSFUL** (2026-09-21).
- **Emulator/manual:** device pending founder pass.
- **Failure/recovery:** stored REACHABLE/UNREACHABLE wins; grant restore is
  presentation-only; missing Asset persists.
- **Known limitation:** a stored OneNote URL that 404s still needs Try Open.
  Discovery absence and CR-08 remain later.
- **Git commit:** pending (do not commit unless asked).

## Later slices (not this change)

1. Discovery absence after a **complete** source page that no longer lists the
   Asset — never from a truncated page.
2. CR-08 — selective deletion with proof of erasure, when its trigger fires.
3. Finer Open causes if the platform can distinguish them honestly.

