# Change control: E4b Universal Sentence Encoder embedder

## Pre-work record

- **Requirement IDs:** ADR-030 E4b; ADR-031 amendment via ADR-032; Spec §4
  EmbeddingEngine, §6 pack download, §11 honesty; enterprise completion §C.
- **Source documents read:** GOVERNANCE, CONTINUE, embedding-first track,
  M1/M2 change controls, ADR-030/031, MediaPipe Text Embedder docs
  (recommended Universal Sentence Encoder).
- **Current-code evidence inspected:** `MediaPipeAverageWordEmbedderSpec` +
  `DownloadOnDeviceEmbeddingModel`; M2 live cosine-only hit@1 **0/3** on
  compact model; E5d boost recovers labeled @1.
- **Open ADRs / limitations:** Emulator M2 does not authorize midrange
  AVAILABLE. E5d token boost remains until USE is re-measured (M3 follow-up).
  EmbeddingGemma deferred (larger/slower).
- **Privacy:** Download model bytes only; no user content; private no-backup
  storage; clear-index deletes model files (including legacy average-word).
- **Vendor choice:** MediaPipe **Universal Sentence Encoder** float32
  (`universal_sentence_encoder/float32/1`) — Google’s recommended semantic
  Text Embedder vs average-word bag-of-words.
- **Smallest safe change:** Product model spec + download/store/disclosure
  copy switch to USE; delete legacy average-word on upgrade; keep E5d assist;
  no AVAILABLE flip; ADR-032.
- **Acceptance criteria:**
  1. Product download installs USE with versioned model identity.
  2. Legacy average-word file is not treated as “already installed.”
  3. Disclosure size/license copy matches USE (larger than 8 MB compact).
  4. Unit tests green; meaning copy still candidate / not measured AVAILABLE.
  5. Docs/CONTINUE/enterprise checklist updated.

## Delivery record

- **Status:** Accepted (engineering) — 2026-08-04
- **Delivered:**
  - ADR-032; `MediaPipeUniversalSentenceEncoderSpec` as product default.
  - Download/store/clear upgrade path; legacy average-word deleted on USE install.
  - Disclosure + Find-by-meaning honesty copy updated (size ~40 MB; rebuild index).
  - Dependency review; unit tests green.
- **Not delivered:** M3 USE page-recall measurement; midrange AVAILABLE; remove
  E5d assist; EmbeddingGemma.
- **Git commit:** (filled at close)
- **User smoke gate:** Download USE on emulator → rebuild meaning index → Find by
  meaning (`mira` page recall).
