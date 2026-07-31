# Change Control — Notes connector (OneNote-class)

**Date opened:** 2026-07-31  
**Status:** Open — plan approved pending; **no Notes implementation code in this
record yet**.  
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
- **Git:** Record after user accepts N0 and local commit of docs.
