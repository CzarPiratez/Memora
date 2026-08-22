# Grounded Answer PDF Slice — Acceptance Specification

**Status:** Accepted acceptance target for the first Grounded Answers vertical slice  
**Authority:** `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`, `docs/GROUNDING_ARCHITECTURE.md`,
ADR-033–ADR-039  
**Updated:** 2026-08-05  

**No production code is authorized by this document alone.** Implementation waits
for remaining readiness gates in `GROUNDING_ARCHITECTURE.md` §14 (model lifecycle
for the chosen reasoner, eval corpus, M4 or explicit device policy, adversarial
pass).

---

## 1. Slice goal

Deliver one offline loop:

```text
User question about indexed PDFs
  → Retriever (stored PDF text / related Memory+page candidates)
  → Evidence Package (bounded, coverage-honest)
  → ReasoningEngine (replaceable; constrained claims)
  → Verifier
  → StructuredAnswer (status + completeness + citations)
  → UI shows answer or abstain + evidence + open cited page
```

Success feels like: **fast path to what my PDF says, with proof** — not a chatbot.

---

## 2. In scope

| Item | Rule |
|---|---|
| Corpus | Current-fingerprint **saved PDF page text** (and Memory evidence derived from it) |
| Task | `ANSWER_QUESTION` only |
| Network | Offline after packs installed; pack download is separate affirmative flow |
| Citations | Required on every accepted claim; open uses cited page locator |
| Abstain | Insufficient, conflicting, truncated/unknown coverage when unsafe to answer |
| Completeness | Always set (`COMPLETE` rare; prefer `PARTIAL`/`UNKNOWN` when unsure) |
| Find | Remains available; may be suggested as next action |

---

## 3. Out of scope (this slice)

- Screenshots, photos, notes as answer corpus
- Chat history / multi-turn memory
- `COMPARE` / `TIMELINE` / `SUMMARIZE` product modes
- Goal graphs (“renew passport”)
- Cloud reasoning
- Marketing AVAILABLE without measured gates
- Numeric confidence
- Reopening PDF bytes during package build or reasoning
- Medical/legal/financial advice framing

---

## 4. Functional acceptance criteria

1. Given a question and indexed PDF text that clearly contains a supporting span,
   the system can return `ANSWERED` with ≥1 claim, each cited to package evidence,
   and open navigates to the cited page.
2. Given no supporting span in the package, status is `INSUFFICIENT_EVIDENCE` (or
   `SHOW_CANDIDATES_ONLY`), never invented prose.
3. Given two cited spans that conflict on the asked fact, status is
   `CONFLICTING_EVIDENCE` with both citations visible.
4. When the package is budget-truncated or deep pages are known unindexed, completeness
   is not `COMPLETE`; UI states the limitation.
5. When the reasoning capability is missing/unsupported, status is
   `CAPABILITY_UNAVAILABLE` with plain language; Find still works.
6. Cancel mid-answer → `CANCELLED`; no partial durable answer; UI recoverable.
7. Process death mid-answer → no restored partial answer; user can retry.
8. Revoked SAF access → honest availability on evidence; no crash; no claim that the
   original file is readable if it is not.
9. Why/answer narrative for this capability comes from StructuredAnswer domain data,
   not ad-hoc Compose string invention of support.
10. Sensitive-topic queries include non-advice limitation copy when policy flags them.

---

## 5. Quality / eval acceptance (before enabling for users)

Minimum fixture set (independently authored):

| Class | Examples |
|---|---|
| Direct fact | Passport expiry / invoice total on a known page |
| Abstain | Question whose answer is absent from corpus |
| Conflict | Two PDFs or pages with disagreeing dates |
| Truncation | Evidence only in a page outside package budget / unindexed tail |
| Paraphrase risk | Wording that could confuse issue vs expiry dates |
| Completeness trap | “Every receipt from Japan” → must not claim `COMPLETE` without exhaustive retrieval |

Release gate metrics (record on midrange when claiming usable):

- Unsupported-claim rate on fixtures = 0 for accepted answers after Verifier
- Abstain on abstain fixtures ≥ policy threshold (target 100% on labeled abstain set)
- No false `COMPLETE` on truncation/exhaustive traps
- Latency / peak RSS / thermal notes recorded (no SLA claim without measurement)

---

## 6. UX acceptance (craft bar)

- One primary job: question → answer or honest refusal
- Evidence inspectable without hunting
- Calm loading / cancel / retry
- No chatbot chrome (avatars, typing bubbles as personality, thread history)
- Accessible: status, limitations, and citations readable by screen readers
- Copy never says “hallucination-proof” or “92% confident”

User-facing feature name remains unfrozen.

---

## 7. Engineering acceptance

- Domain contracts free of Android/Room/Compose/model SDK types
- Hilt binds interfaces; test fakes for Retriever, Package Builder, Reasoner, Verifier
- Unit tests for package limits, verifier reject paths, status mapping
- Instrumentation: cancel, process recreation, capability unavailable
- No Room transaction held across inference
- Single-flight reasoner access in v1

---

## 8. Exit to “slice complete”

All of:

- §4 functional criteria met on emulator + at least one physical midrange policy path
- §5 eval fixtures green for the enabled device tier
- §6 UX review signed off by product owner (user)
- Remaining §14 gates in `GROUNDING_ARCHITECTURE.md` closed for this capability
- CHANGELOG + CONTINUE updated; local git checkpoint after verification

Widening corpus or tasks requires a new acceptance spec / ADR — do not silently expand.
