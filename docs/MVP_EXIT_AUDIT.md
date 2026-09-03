# MVP exit audit

**Status:** Living inventory — update when evidence changes  
**Opened:** 2026-09-01  
**Authority:** Informs MVP exit gate in `docs/POST_MVP_PROGRAM_V1.md` §2. Does
**not** authorize marketing **AVAILABLE** or post-MVP phases by itself.  
**Marketing AVAILABLE:** **NO** until founder/product review after engineering
gates pass (`docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` §B).

## How to read this audit

| Verdict | Meaning |
|---|---|
| **PASS** | Evidence on record satisfies the row for MVP exit purposes |
| **PARTIAL** | Engineering exists but evidence incomplete (device tier, offline, or freshness) |
| **FAIL** | Not met; blocks honest MVP exit / AVAILABLE |
| **OPEN** | Explicitly deferred out of MVP exit scope (not a blocker) |

Each row cites **evidence pointers** — not conversational memory.

---

## A. Recall and Find (Canonical Recall path)

| Row | Verdict | Evidence / notes |
|---|---|---|
| Canonical Recall as sole Find boundary | **PASS** | `docs/RECALL_CONVERGENCE_DONE.md` COMPLETE; `docs/LEGACY_RECALL_SURFACE.md` Live/Dual **N = 0** |
| Keyword Find — PDF | **PASS** | MIG-07 PDF cutover; convergence program |
| Keyword Find — photo / screenshot | **PASS** | MIG-07 cutovers delivered |
| Keyword Find — note (OneNote) | **PASS** | `docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md` N0–N7 accepted |
| Find by meaning — plumbing | **PASS** | Page embeddings E5c; MIG-05 claim B all MVP types; anchor fix 2026-09-01 |
| Find by meaning — emulator E2E | **PASS** | `CHANGE_CONTROL_MEANING_SEARCH_ANCHOR_FIX.md`; CONTINUE: `invoice` / `mira` on emulator |
| Find by meaning — physical device E2E | **PASS** | `docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md` — Samsung SM-A156E; offline meaning Find + Why + Open original (2026-09-02) |
| Shared result + Why contract | **PASS** | `CanonicalRecallResult` + `CanonicalRecallWhyCopy`; `CANONICAL_RECALL_RESULT_CONTRACT.md` |
| STALE / reach deadlock regression | **PASS** | `CHANGE_CONTROL_RECALL_REACH_STALE_EVIDENCE_DEADLOCK.md` |
| Meaning search anchor crash (≥4 char queries) | **PASS** | `CHANGE_CONTROL_MEANING_SEARCH_ANCHOR_FIX.md` (2026-09-01) |

---

## B. Honesty and trust surfaces

| Row | Verdict | Evidence / notes |
|---|---|---|
| Corpus completeness UI (indexed / pending / blocked) | **PASS** | FC-04 delivered; `CHANGE_CONTROL_FC04_CORPUS_COMPLETENESS_HONESTY.md` |
| Candidate / not measured AVAILABLE copy on meaning path | **PASS** | Enterprise completion + FC-04; CONTINUE explicitly "Not AVAILABLE" |
| Explain cites stored evidence (Why) | **PASS** | Phase 4 exit gate; convergence program |
| Evidence lineage in Why (model id, revision in UI) | **PARTIAL** | FC-06 **candidate** — not delivered |
| Read-only originals | **PASS** | Product Contract invariant; enforced in architecture |
| Silent keyword-as-meaning | **PASS** | Governance + honesty copy; degraded modes explicit |

---

## C. Local AI and measurement gates

| Row | Verdict | Evidence / notes |
|---|---|---|
| AI Pack install + meaning index build | **PASS** | USE model path; M3/M4 measurement records |
| PDF page recall measurement (fixture corpus) | **PASS** | M1 JVM; M2 emulator; M3 USE emulator; M4 midrange execute |
| Marketing AVAILABLE / SLA claim | **FAIL** | Enterprise completion §B — founder decision **open** |
| **A-01** offline end-to-end (airplane mode after pack) | **PASS** | `docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md` — Samsung SM-A156E; keyword + meaning Find, Why, Open original offline (build `bf511ca`) |
| Battery / latency budgets (index + query) | **FAIL** | Enterprise completion §C — open |
| Emulator-only as AVAILABLE host | **FAIL** (by policy) | Benchmark plan: midrange required before AVAILABLE claim |

---

## D. MVP asset types and connectors

| Row | Verdict | Evidence / notes |
|---|---|---|
| Photos / screenshots discovery + Memory | **PASS** | ROADMAP Phase 1–3 delivered paths |
| PDF discovery + extraction + Memory | **PASS** | SAF + local reading progress; recall track |
| Notes — OneNote connector E2E | **PASS** | N0–N7 closed in connector change control |
| Audio / video / meetings | **OPEN** | Post-MVP P5; PRD exclusion until ADR |
| Drive / iCloud / NAS connectors | **OPEN** | Post-MVP P5 |

---

## E. Post-MVP program prerequisites (foundations)

| Row | Verdict | Evidence / notes |
|---|---|---|
| `POST_MVP_PROGRAM_V1.md` | **PASS** | Opened 2026-09-01 |
| Class A conformance validator + CI | **PASS** | `CHANGE_CONTROL_CLASS_A_CONFORMANCE_VALIDATOR.md`; public publish SHA `7c573b2` on https://github.com/CzarPiratez/unfynd-core (2026-09-02) |
| `:core-domain` Gradle module | **FAIL** | `CORE_APP_SEPARATION_PLAN.md` Phase 2 not started |
| Grounding domain interfaces (no runtime) | **PARTIAL** | Slice 1: `domain.grounding` contracts landed; generative runtime deferred |
| `RecallRanker` port for FC-02 / ADR-051 | **PARTIAL** | Slice 1 + ADR-051 + Stage A model brief accepted; S1 spike scaffolding landed; Stage A **not wired** into Find; Stage B not started; ADR-052 launch UX policy only |
| Locator / modelSignature / domain CorpusCoverage | **PARTIAL** | `AudioSegmentEvidenceLocator`, `EmbeddingModelSignature`, `domain.grounding` contracts landed; `CorpusCompleteness` already in domain (FC-04) |

---

## F. Blockers summary (honest)

### Blocks marketing AVAILABLE today

1. **Product AVAILABLE decision** — enterprise completion §B (founder review).
2. **Battery / latency budgets** not recorded.

### Closed this week (do not re-open as AVAILABLE blockers)

- A-01 offline device proof — **PASS**
- F-01–F-03 Canonical Recall quality — **PASS** (device verified; `CHANGE_CONTROL_CANONICAL_RECALL_QUALITY_F01_F03.md`)
- F-04 SAF PDF folder rescan — **PASS** (device verified)
- Class A pack publish to public `unfynd-core` — **done** (`7c573b2`)

### Does not block engineering post-MVP prep

- Domain foundations (remaining §7 items; `:core-domain` Phase 2).
- FC-02 / ADR-051 Stage A (authorized; model brief accepted; S1 device proof
  pending — see `docs/FC02_STAGE_A_S1_SPIKE_RUNBOOK.md`). Stage B is post-MVP
  differentiator (needs FC-03); not an MVP-exit gate alone.
- ADR-052 smart automatic pack onboarding (accepted policy; unified UI not shipped).

### Explicitly out of MVP exit

- Connect, Grounded Answers runtime, Act, audio/video, cloud connectors.

---

## G. Recommended next engineering actions (from audit)

Ordered by **dependency**, not calendar:

1. ~~Close Class A validator slice~~ **DONE** (validator + public publish `7c573b2`).
2. ~~Close SAF rescan (F-04) and Canonical Recall quality (F-01–F-03)~~ **DONE** (device PASS).
3. ~~Accept ADR-051 Evidence-native RecallRanker~~ **DONE** (docs).
4. ~~Accept Stage A model brief + ADR-052 smart automatic onboarding policy~~ **DONE** (docs).
5. **Run S1 spike on Samsung** (`docs/FC02_STAGE_A_S1_SPIKE_RUNBOOK.md`) → S2 fixture →
   Stage A wire into `AnchorAwareMeaningRecallRanking` (ORT still androidTest-only until S1 green).
6. Record **battery / latency budgets** (enterprise completion §C).
7. Founder **AVAILABLE** review only after §F blockers addressed; launch onboarding
   UI (ADR-052) after Stage A wire — not before.
8. Optional App risk (not MVP-exit gate): F-05 large SAF folder hang.

---

## Document history

| Date | Change |
|---|---|
| 2026-09-03 | Docs track check: Live/Dual N=0 operator sync; ADR-052 recorded; next = S1 device spike |
| 2026-09-02 | ADR-051 accepted; next = FC-02 Stage A (measured pack) |
| 2026-09-02 | F-01–F-04 device PASS; Class A public publish `7c573b2`; next = FC-02 slice 2 |
| 2026-09-02 | A-01 PASS on Samsung SM-A156E (`CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md`) |
| 2026-09-02 | Domain foundations slice 1; FC-02 authorized; A-01 runbook |
| 2026-09-01 | Initial audit opened |
