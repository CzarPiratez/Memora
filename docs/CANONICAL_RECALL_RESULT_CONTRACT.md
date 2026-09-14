# Canonical Recall — shared result + Why contract

**Status:** **Implemented** in App (2026-08-31). `CanonicalRecallResult` +
`CanonicalRecallWhyCopy`; keyword hits carry `recall`; meaning path maps via
`toCanonicalRecallResult()`. Thin KEYWORD façade + MIG-07B meaning/hybrid path
landed earlier. Does **not** claim marketing AVAILABLE or Grounded Answers.

**Authority:** Architecture Freeze §3 (one evidence substrate; one canonical
recall pipeline; explainability); ADR-049 (Canonical Recall naming); ADR-024
(no silent keyword-as-meaning); `docs/RECALL_CONVERGENCE_DONE.md` (shared
result + Why exit boxes); Grounding Architecture §6–7 (labeled candidates;
Evidence Package hints — **do not implement Grounded Answers here**).

**Updated:** 2026-09-14

This file does **not** authorize MIG-06+. It freezes a minimal shared hit/Why
shape so MIG-06/07 do not invent four Explain dialects.

---

## 1. Scope

| In scope | Out of scope |
|----------|--------------|
| App **Find** results **after** Canonical Recall exists as the product-facing retrieval API (ADR-049 / MIG-07 cutover) | Grounded Answers `StructuredAnswer` / Evidence Package builder (may share path labels later; **do not merge constitutions**) |
| Logical field names + honesty rules for hits and Why | Kotlin data classes, UI redesign, single Find screen |
| Path labels for candidate → result honesty | `RecallRanker` implementation, ranking quality bars |

`RECALL_CONVERGENCE_DONE` “shared result” / “shared Why” boxes are satisfied
**only** when the App implements this contract (or a superseding ADR).

---

## 2. Shared result (logical fields)

Every **user-visible Find hit** after Canonical Recall cutover MUST be
expressible with the fields below. Field names are logical — not Kotlin.

| Field | Required? | Notes |
|-------|-----------|-------|
| `memoryId` | Yes when Memory-backed; **required after MIG-07 keyword cutover** for product hits | Ties the hit to the Memory substrate (Freeze §3). |
| `revisionId` | Same as `memoryId` | Active revision the evidence was read from. |
| Asset identity | Yes | Enough to open the original: `sourceId`, `sourceAssetKey`, `assetType`. |
| Display `label` | Yes | User-facing title; not a second truth store. |
| Primary `excerpt` | Yes | From `MemoryEvidence` text or ranked evidence text already stored — **not** a parallel truth rebuilt from extraction tables at Find time. |
| `evidenceId`(s) and/or `EvidenceLocator` | Yes for Why | What Why cites; must resolve to stored evidence / locator. |
| `retrievalPath` | Yes | At least `KEYWORD` \| `MEANING` (extensible later: e.g. `ANCHOR_FILTER`). Opaque enum/label for honesty — not marketing confidence. |
| Score / rank signal | Optional | Opaque ranking signal for ordering; **do not invent user-facing confidence**. |
| Open-original hints | When locator supports | E.g. PDF page from `PdfPageEvidenceLocator`; must not invent a page not cited by stored locator/evidence. Find cards may show a read-only thumbnail of that same original (matched page when known); the thumbnail is presentation, not a ranking input, and a missing preview must not drop the hit. After Open, pinch-zoom may re-render that same original at a larger on-device decode budget; zoom is presentation, not a ranking input. |

### Path label vocabulary (v1 minimum)

| Label | Meaning |
|-------|---------|
| `KEYWORD` | Literal / keyword candidate generation matched stored text |
| `MEANING` | Embedding / meaning candidate generation ranked this hit |

Later extensions (e.g. `ANCHOR_FILTER`) may be added by ADR or MIG-07B without
renaming these two. A hit must not be labeled `MEANING` when only keyword
matched (ADR-024).

---

## 3. Why contract (logical)

Same Why shape for **all asset types** after cutover. Logical requirements:

1. **Cite stored evidence** — The **hit as a whole** must surface the stored
   excerpt (or equivalent span) that justified the result. Never invent facts,
   pages, or phrases not present in stored evidence / authorized extracts
   already on device. Why itself quotes that span **only when the result card
   does not already show it** (U5 / defect D-17): keyword cards render the
   matching excerpt, so Why does not repeat it; meaning cards do not dump OCR,
   so Why quotes the justifying line. Do not print the file name or page in
   Why when those already sit on the card.
2. **Surface `retrievalPath` honestly** — Keyword hits must be presented as
   keyword (or `KEYWORD`); meaning hits as meaning (or `MEANING`). No silent
   keyword-as-meaning (ADR-024).
3. **Do not imply unconfirmed anchors were matched** — Align Freeze
   explainability: per constraint, state satisfied / unmatched / unavailable;
   never imply an unconfirmed cue was confirmed. MIG-07B may refine
   anchor-stage Why detail later; this contract forbids false confirmation now.
4. **One dialect** — PDF, photo, screenshot, and note Finds share this Why
   shape after cutover; do not keep four asset-specific Explain constitutions.
   App presentation: one `WhyPresentation` (relevance, optional cited line,
   how-found) assembled by `CanonicalRecallWhyCopy` and rendered by one
   `WhyDisclosure` on every Find card. Keyword and meaning share the slots;
   they must not grow a second visual treatment. Hidden-word honesty for a
   partial meaning list stays on the result-list banner, not on every card.

Why is a presentation of the shared result’s evidence + path labels. It is
**not** Grounded Answers claim prose.

---

## 4. Candidate generation vs result

```text
KEYWORD / MEANING / evidence / anchor stages
        → candidates (labeled by retrievalPath)
        → shared ranking inside Canonical Recall
        → ordered Canonical Recall results (this contract)
        → UI binds only to those results (after MIG-07)
```

- Keyword, meaning, evidence-level embedding, and future anchor stages are
  **candidate generation**, not independent product Find systems (ADR-049).
- Shared ranking produces the ordered list of Canonical Recall results.
- After MIG-07 cutover, UI must **not** bind product Find cards to
  `SearchPersisted*` / per-type `*KeywordSearchHit` / interim
  `MeaningSearchHit` as the long-term contract — those remain legacy until
  retired per `LEGACY_RECALL_SURFACE`.

---

## 5. Non-goals

- No Kotlin data classes in this step
- No UI redesign / single Find screen requirement
- No `RecallRanker` implementation
- No Grounded Answers Evidence Package builder / `StructuredAnswer`
- Does **not** authorize MIG-06+
- Does **not** add or extend `LEGACY_RECALL_SURFACE` rows
- Does **not** claim marketing AVAILABLE / ranking quality

---

## 6. Mapping: today’s hits → shared fields

Read-only gap note for MIG-06/07. “Covered” means the logical field already
exists on the interim DTO (possibly under another name).

| Today’s type | Covered today | Gaps vs this contract |
|--------------|---------------|------------------------|
| `MeaningSearchHit` | `memoryId`, `revisionId`, asset identity, `label`, score, open-page hints (`citedPdfPageNumber` / `rankedPdfPageNumber`), prose Why via copy | No structured `evidenceId` / `EvidenceLocator` on the hit; no `retrievalPath` field (path implied by screen); primary text is `summaryText` (Memory summary), not always the ranked `MemoryEvidence` excerpt |
| `PdfKeywordSearchHit` | `label`, `excerpt`, `sourceId`, `sourceAssetKey`, page open hint (`pageNumber`); Why copy says keyword | **No `memoryId` / `revisionId`**; **no `evidenceId` / locator**; **no `retrievalPath` field** (honesty only in UI strings); no `assetType` on DTO; no score |
| `PhotoOcrKeywordSearchHit` | `label`, `excerpt`, `sourceId`, `sourceAssetKey`; Why copy says keyword | Same core gaps: **no Memory ids**, **no `evidenceId`**, **no `retrievalPath` field**, no `assetType`, no score, no structured locator |
| `ScreenshotOcrKeywordSearchHit` | Same as photo OCR keyword | Same core gaps as photo |
| `NotePageKeywordSearchHit` | Same as photo OCR keyword | Same core gaps as photo |

**Key convergence gap:** keyword hits today can show an excerpt and honest
copy, but they do **not** carry `evidenceId` / `EvidenceLocator` or a
structured `retrievalPath` on the hit — and they are not Memory-revision
backed. After MIG-07 keyword cutover, product hits must be Memory-backed and
Why-citable via stored evidence, with path labels on the shared result (not
only in per-screen string builders).

---

## 7. Relationship to Grounded Answers

Grounding Retriever (ADR-049 Option C) may later consume the **same** labeled
candidate pipeline. Evidence Package §7 requires retrieval-path labels per
candidate — compatible with `retrievalPath` here.

This contract does **not** define Evidence Package contents, Verifier rules, or
`StructuredAnswer`. Find Why and Ask citations may share labels; they remain
separate constitutions.

---

## 8. Satisfaction of RECALL_CONVERGENCE_DONE

Check the “Shared result model exists” and “Shared Why / evidence presentation
exists” boxes in `docs/RECALL_CONVERGENCE_DONE.md` only when App code
implements this contract (or a superseding ADR). This DRAFT sketch alone does
**not** check those boxes.
