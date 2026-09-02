# Change control — A-01 offline end-to-end device proof

**Date:** 2026-09-02  
**Type:** Verification / governance — physical device evidence  
**Status:** Accepted — A-01 **PASS** on recorded device  
**Requirement IDs:** A-01 (`docs/LOCAL_AI_BENCHMARK_PLAN.md`, `docs/ROADMAP.md`, `docs/PRD_TRACEABILITY.md`)  
**Runbook:** `docs/A01_OFFLINE_DEVICE_PROOF_RUNBOOK.md`

Does **not** authorize marketing **AVAILABLE**, Act, Grounded Answers, or closure of
battery/latency budget rows.

## Pre-work record

- **Source documents read:** `docs/GOVERNANCE.md`, `docs/PRODUCT_SOURCE_REGISTRY.md`,
  `docs/LOCAL_AI_TECHNICAL_SPEC.md` §9–10, `docs/A01_OFFLINE_DEVICE_PROOF_RUNBOOK.md`,
  `docs/MVP_EXIT_AUDIT.md`, `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md`,
  `CONTINUE.md`.
- **Current-code evidence inspected:** Canonical Recall meaning + keyword paths;
  AI Pack USE install; meaning index build; `CanonicalRecallWhyCopy`; offline
  contract in Local AI Spec §10.
- **Open ADRs / platform limitations checked:** Meaning path remains candidate /
  not measured AVAILABLE per FC-04 and enterprise completion §B.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Proof used user-owned spelling-list PDFs on device. No repository secrets.
  Model downloaded once from Google MediaPipe hosting; all Find/Why steps ran
  offline after install.
- **Smallest safe change:** Record device proof in change control; update MVP
  audit row A-01; no product code in this slice.
- **Acceptance criteria:**
  - [x] Physical arm64 device (midrange class).
  - [x] AI Pack (USE) installed; meaning index built while online once.
  - [x] Airplane mode; cold start; keyword + meaning Find; Why; Open original.
  - [x] All five runbook pass criteria **Yes**.
  - [x] Does not claim AVAILABLE or close unrelated audit rows.

## Architectural convergence

N/A — verification record only; no Find/Recall code change in this slice.

## Delivery record — device proof

### Host

| Field | Value |
|---|---|
| Device | Samsung **SM-A156E** (Galaxy A15 class, midrange) |
| Android version | *(operator: confirm in Settings → About phone if re-auditing)* |
| App build (git SHA) | `bf511cae0cfbeda1694b3e2e42e38359366b6449` |
| Proof date (local) | 2026-09-02 |
| Network during offline steps | Airplane mode (Wi‑Fi and mobile data off) |

### Corpus and setup (online, once)

1. Debug build installed from Android Studio Run on physical device.
2. PDF folder: `Documents/unfynd-test` (3 spelling-list PDFs).  
   **Note:** `Downloads/Adobe Acrobat` hung on SAF metadata scan on this device;
   small folder used instead (follow-up: SAF rescan / large-folder performance).
3. Connect PDF folder → Index → Local PDF reading → Done.
4. Build memories from saved facts → **3** Asset Memories.
5. About on-device meaning search → download USE model → Build meaning index
   (**3** memory summaries, **3** evidence vectors).

### Online smoke (before airplane mode)

| Path | Cue | Result |
|---|---|---|
| Keyword Find (saved PDF text) | `scan`, `silky`, `wreck` | Hits with excerpt + Why |
| Meaning Find | `learning spelling of English words` | Multiple PDF memories + Why |

### Offline proof (A-01)

After airplane mode + force-stop + cold start:

| Runbook criterion | Result |
|---|---|
| Keyword Find returns hits offline | **Yes** (`scan`) |
| Meaning Find returns hits offline | **Yes** (3 results; also natural-language cue) |
| Why shows stored evidence | **Yes** (excerpt + PDF label; meaning-similarity path label) |
| Open original works | **Yes** (read-only PDF preview) |
| No crash / ANR | **Yes** |

**A-01 verdict: PASS**

### Evidence pointers

- Operator screenshots: meaning Find offline, Why expanded, keyword `scan` offline.
- Unit suite on same build era: **467/467** `testDebugUnitTest` (Android Studio, prior session).

### Known limitations / follow-up (do not invalidate A-01)

| ID | Issue | Tracking |
|---|---|---|
| F-01 | Keyword Find treats multi-word input as one phrase (no per-file AND) | Canonical Recall keyword path |
| F-02 | Meaning Find can rank semantically similar files missing explicit cue tokens | Lexical constraint filter in Canonical Recall |
| F-03 | Why copy exposes engineering phrasing (“on-device meaning similarity”) | `CanonicalRecallWhyCopy` consumer rewrite |
| F-04 | SAF PDF folder does not rescan after checkpoint complete | **Delivered** — `CHANGE_CONTROL_SAF_PDF_FOLDER_RESCAN.md` (device re-verify) |
| F-05 | Large `Downloads` subtree hung metadata indexing on Samsung | SAF performance investigation |
| F-06 | Battery / latency budgets | Enterprise completion §C — still open |
| F-07 | Marketing AVAILABLE | Founder decision — still open |

## Documentation / traceability updates

- `docs/MVP_EXIT_AUDIT.md` — row **A-01** → **PASS**; physical meaning E2E row updated.
- `docs/PRD_TRACEABILITY.md` — A-01 evidence pointer.
- `CONTINUE.md` — checkpoint note.

## Git commit

Pending operator request.
