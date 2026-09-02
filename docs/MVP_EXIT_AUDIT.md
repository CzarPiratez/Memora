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
| Find by meaning — physical device E2E | **PARTIAL** | M4 midrange PDF page recall measured (Galaxy A15); **full App offline meaning path on physical device after 2026-09-01 fixes not re-recorded in change control** |
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
| **A-01** offline end-to-end (airplane mode after pack) | **PARTIAL** | `docs/LOCAL_AI_BENCHMARK_PLAN.md` §Offline; `docs/ROADMAP.md` lists A-01 **open** — no change-control record closing full create→Find→Why offline proof on physical device |
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
| Class A conformance validator + CI | **PASS** | `CHANGE_CONTROL_CLASS_A_CONFORMANCE_VALIDATOR.md`; validator self-test green |
| `:core-domain` Gradle module | **FAIL** | `CORE_APP_SEPARATION_PLAN.md` Phase 2 not started |
| Grounding domain interfaces (no runtime) | **FAIL** | P3 foundation — not yet landed |
| `RecallRanker` port for FC-02 | **PARTIAL** | Slice 1: `rank` API + `IdentityRecallRanker` landed; cross-encoder slice 2 not wired |
| Locator / modelSignature / domain CorpusCoverage | **PARTIAL** | `AudioSegmentEvidenceLocator`, `EmbeddingModelSignature`, `domain.grounding` contracts landed; `CorpusCompleteness` already in domain (FC-04) |

---

## F. Blockers summary (honest)

### Blocks marketing AVAILABLE today

1. **Product AVAILABLE decision** — enterprise completion §B (founder review).
2. **A-01** full offline end-to-end proof on physical device not closed.
3. **Battery / latency budgets** not recorded.
4. **Physical device re-validation** of meaning Find after 2026-09-01 anchor fix recommended before AVAILABLE.

### Does not block engineering post-MVP prep

- Class A validator (in progress).
- Domain foundations (P1–P2 prep).
- FC-02 change control (after explicit authorization).

### Explicitly out of MVP exit

- Connect, Grounded Answers runtime, Act, audio/video, cloud connectors.

---

## G. Recommended next engineering actions (from audit)

Ordered by **dependency**, not calendar:

1. Close **Class A validator** slice (public pack runnable proof).
2. Record **A-01** offline proof on physical device (change control + log).
3. Re-run **meaning Find E2E** on physical device post anchor fix; attach evidence.
4. Implement **FC-02 slice 2** cross-encoder; wire into `AnchorAwareMeaningRecallRanking`.
5. Publish Class A pack per `docs/PUBLISH_CLASS_A_PACK.md`.
6. Founder **AVAILABLE** review only after rows in §F blockers 1–4 addressed.

---

## Document history

| Date | Change |
|---|---|
| 2026-09-02 | Domain foundations slice 1; FC-02 authorized; A-01 runbook |
| 2026-09-01 | Initial audit opened |
