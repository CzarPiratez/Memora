# SPEC status vs UNFYND App (honest)

**Pack revision:** 2026-08-30  
**License:** Apache License 2.0  
**Purpose:** Map Class A contract areas in [`SPEC.md`](SPEC.md) to what the
**UNFYND App** surfaces today — without claiming the App or private monorepo is
open.

Statuses are maintenance labels for readers, not SLAs:

| Label | Meaning |
|---|---|
| **shipped** | Present in current App product surfaces in a usable form |
| **partial** | Contract seam or subset exists; gaps remain |
| **not shipped** | Direction or future; do not treat as available |

Do **not** read marketing **AVAILABLE** meaning-recall / SLA numbers from this
table. That bar is a separate measured product decision and is **not claimed**.

---

## Status table

| Contract area | In public SPEC | UNFYND App today | Notes |
|---|---|---|---|
| Core noun / local-first / no cloud on core path | Yes (§1) | **partial** | App path is local-first after required on-device capability; optional cloud is not the Core path. Private App implementation not opened. |
| Asset → Memory → evidence model | Yes (§2) | **shipped** (Asset Memory foundation) | Originals stay with user/OS; Memory is not a copy of the file. |
| Memory integrity (stable id; honest incomplete) | Yes (§2.1) | **partial** | Identity and assembly outcomes exist; full revision/history UX is not the Class A claim. |
| Evidence classes (DIRECT / VALIDATED_OBSERVATION / RETRIEVAL_SIGNAL / HYPOTHESIS) | Yes (§3) | **partial** | Classes exist on Asset Memory evidence. Current assembly tags deterministic extraction as **DIRECT**. Observation / link / signal classes are not driving ranking or Links UI. |
| Truth-before-intelligence / evidence-first / retrieval-first | Yes (§4) | **partial** | Ordinary Find cites stored evidence; do not invent unsupported facts. Generative Ask / Grounded Answers code not shipped. |
| Capability seams (Vision, OCR, Document, Embedding, MemoryBuilder, RecallRanker) | Yes (§5) | **partial** | OCR / document extract / embedding / deterministic MemoryBuilder / keyword + candidate meaning recall paths exist in App. **VisionEngine** not consumed into Memory assembly. |
| MemoryBuilder assemble contract | Yes (§5, §5.1) | **partial** | `assemble` seam live: facts + optional local observations → schema-validated Memory (or honest build outcome). Production assembly is **deterministic**; observations default empty until Vision. |
| Ordinary Find / recall + Why (explain from stored evidence) | Yes (§4 principles; Find as capability) | **shipped** (keyword Find + Why); **partial** (Find by meaning) | **Keyword Find** is shipped on unified **Memory-evidence** literal search (asset-type UI surfaces). It is **not** four separate extraction-table search engines. **Find-by-meaning** is a **candidate** path: evidence-level embeddings support retrieval (including a PDF/page evidence path); summary embeddings remain. Not a marketing AVAILABLE / SLA claim. **Canonical Recall** is the named future App Find boundary; it is **not** a single live App API yet. |
| Staged Link / Event / Knowledge Memories | Yes (§6 as future) | **not shipped** | Approved direction only. |
| Low-power = event-driven Memory lifecycle | Yes (§7) | **partial** | Product interpretation (batch / device-aware work); not neuromorphic hardware. |
| Privacy high-level (read-only originals, consent, revoke) | Yes (§8) | **partial** | Core rules apply; connector/source coverage varies by surface. |
| Act / agents (act on user’s behalf) | Explicit non-claim (§9) | **not shipped** | Out of current architecture until a later product-contract change. |
| Grounded Answers / generative Ask UI | Non-claim (§9); future | **not shipped** | Architecture may exist privately; generative implementation not claimed shipped. |
| Marketing AVAILABLE semantic recall / SLA | Non-claim (§9) | **not shipped** / **not claimed** | Physical midrange measurement exists; midrange measured ≠ marketing AVAILABLE. Product AVAILABLE decision remains open. |
| Release Class B / App open source | Non-claim | **not shipped** | Separate later ADR. |

---

## How to read this with examples

[`examples/`](examples/) are **synthetic**, illustrative sketches under the same
Apache-2.0 license. They are not a production schema lock and not personal data.
See [`examples/README.md`](examples/README.md).
