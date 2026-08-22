# Change control: UNFYND identity Step 2 — architecture registry

**Date:** 2026-08-23  
**Type:** Documentation / architecture inventory and registration  
**Decision guardrails:** Docs only. No constitution product-noun overlay (Step 3).
No Kotlin, Gradle, UI, applicationId, database, or GitHub. No Grounded Answers or
Event/Knowledge Memory implementation. No ADR-041 (no constitution conflict).

## Pre-work record

- **Requirement IDs:** P-01 / P-18 (scope control); A-02 (no new cloud path);
  G-01–G-08 (architecture accepted, not implemented); E-01–E-06 (PKI staged).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`,
  `CONTINUE`, `PRODUCT_CONTRACT`, `ARCHITECTURE`, `DECISIONS` (ADR-040), `ROADMAP`,
  `PRD_TRACEABILITY`, `LOCAL_AI_TECHNICAL_SPEC`,
  `UNFYND_IDENTITY_TRANSITION_PLAYBOOK` (Step 2 only), `CHANGE_CONTROL_TEMPLATE`.
- **Current-code evidence inspected:** Git status; `git ls-files` for architecture
  Markdown; grep for a second Grounded Answers “constitution”; no user-supplied
  UNFYND architecture files in the repo or this prompt. `applicationId` not edited.
  Three `.docx` SHA-256 values match the registry table.
- **Open ADRs / platform limitations checked:** ADR-040 binds identity. ADR-018/019
  PKI and ADR-033–039 grounding remain architecture. No competing unmarked
  constitution. Technical IDs stay deferred.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Inventory + track frozen Grounded Answers files as-is +
  SHA-256 of Git blobs for accepted Markdown constitutions/amendments/slice spec +
  CONTINUE read-order/checkpoint pointers + this record + one Unreleased changelog
  bullet.
- **Acceptance criteria:**
  - Inventory in this record with required classifications.
  - Registered files tracked in Git; SHA-256 match hashed blobs.
  - One Grounded Answers constitution (`GROUNDING_ARCHITECTURE.md`); amendment is
    not a second constitution.
  - Three `.docx` SHA-256 rows byte-identical to prior registry table.
  - No `MemoraApp/` identity edits; no Step 3 overlay; local commit; no push.
- **Test and emulator verification plan:** Docs inspection and hash verification.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Inventory (architecture-related cluster)

No additional user-supplied UNFYND architecture files were provided. In-repo
cluster only. `docs/ARCHITECTURE.md` / `docs/PRODUCT_CONTRACT.md` /
`docs/GOVERNANCE.md` remain living canon from the existing read-order; they are
**not** newly hashed this step (product-noun overlay is Step 3).

| File | Git state before this step | Classification | Register / hash |
|---|---|---|---|
| `docs/GROUNDING_ARCHITECTURE.md` | Untracked | **Canon** (engineering constitution; sole Grounded Answers constitution) | Yes — SHA-256 in registry |
| `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` | Untracked | **Amendment** (product-direction) | Yes — SHA-256 in registry; amendment row honest because file is in Git |
| `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` | Untracked | **Acceptance / slice spec** | Yes — SHA-256 in registry |
| `docs/CHANGE_CONTROL_GROUNDED_ANSWERS_ARCHITECTURE.md` | Untracked | **Change-control / changelog** (not product source) | Track as-is; **do not** hash as product source |
| `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` | Tracked; dirty §10 status refresh (GA-aligned, not identity overlay) | **Amendment** (product-direction; already registry-listed) | Yes — SHA-256 of committed blob (includes §10 refresh) |
| `docs/LOCAL_AI_TECHNICAL_SPEC.md` | Tracked; dirty v1.3 §9 Grounded Answers carve-out (not identity overlay) | **Canon** (engineering constitution; already read-order canon) | Yes — SHA-256 of committed blob (includes v1.3 carve-out) |
| `docs/ARCHITECTURE.md` | Tracked; dirty GA capability-map pointer | **Canon** (living engineering map) | Do not register as a new hashed artifact this step |
| `docs/PRODUCT_CONTRACT.md` | Tracked; dirty PKI/GA pointer hunks | **Canon** (living product contract) | Do not hash this step (Step 3 overlay) |
| `docs/PRD_TRACEABILITY.md` | Tracked; dirty G-01–G-08 rows | Traceability matrix (not a constitution) | Do not hash this step; extra hunk reported |
| `docs/ROADMAP.md` | Tracked; dirty GA roadmap hunks | Roadmap (not a constitution) | Do not hash this step; extra hunk reported |
| `docs/CHANGELOG.md` | Tracked (HEAD already had a GA Unreleased note while files were untracked) | **Change-control / changelog** | Not hashed; Step 2 Unreleased bullet added |
| `docs/product-source/*.docx` | Tracked immutable baselines | Historical PRD / addenda | SHA-256 rows unchanged |
| `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md` | Tracked (Step 0) | Operating procedure, not architecture freeze | Not hashed as architecture constitution |

**Constitution uniqueness:** Only `docs/GROUNDING_ARCHITECTURE.md` claims to be
the Grounded Answers constitution. The amendment cites it as the canonical
engineering map. No merge or winner-picking was required.

## Hashes recorded (Git blob as committed)

| Artifact | SHA-256 |
|---|---|
| `docs/LOCAL_AI_TECHNICAL_SPEC.md` | `7EF9F38F0C33708A7E1E4CC7E21A2D062F037F3471CC4C68A782BB1CEC621DDD` |
| `docs/GROUNDING_ARCHITECTURE.md` | `5D667ADAA3505044FC541AEDA38AE10262B832D1023ECA228406A62DC994EDB0` |
| `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` | `ED25CDE8FA1DBE2B7EFC422276C8D1A0E5F7A8B9A5339062A6AF2F701796E06A` |
| `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` | `0B1BA90F8E8943991C2B6640CB67150763CEF51BFF557CFE785A6E3003A1C75A` |
| `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` | `E39CAC711ACA0C571E24A4C91777B879065DECE565DAF4DAC592F74A8DDD4E2F` |

Bodies of Grounded Answers files were not rewritten Memora→UNFYND this step.

## Delivery record

- **Files/layers changed:**
  - `docs/GROUNDING_ARCHITECTURE.md` (add as-is)
  - `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` (add as-is)
  - `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` (add as-is)
  - `docs/CHANGE_CONTROL_GROUNDED_ANSWERS_ARCHITECTURE.md` (add as-is; non-source)
  - `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` (commit existing §10 honesty refresh)
  - `docs/LOCAL_AI_TECHNICAL_SPEC.md` (commit existing v1.3 §9 carve-out)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (honest amendment row + governed artifacts table)
  - `CONTINUE.md` (read-order + checkpoint identity line only among this step’s intent)
  - `docs/CHANGELOG.md` (one Unreleased bullet)
  - `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP2_ARCHITECTURE_REGISTRY.md` (new)
- **Automated verification and result:** Docx hashes match registry. Markdown SHA-256
  computed from `git show :<path>` blobs after add.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:**
  - Playbook Step 3 product-noun overlay not started.
  - `CONTINUE.md` still has extra dirty hunks from the uncommitted Grounded Answers
    docs landing (status table row, authoritative-constitution pointers, next-eng
    parallel workstreams, ADR-033–039 freeze paragraph). Those were not rewritten
    as overlay; only read-order and checkpoint identity lines were edited for Step 2.
  - Left unstaged (not this checkpoint): `docs/ARCHITECTURE.md`,
    `docs/PRODUCT_CONTRACT.md`, `docs/PRD_TRACEABILITY.md`, `docs/ROADMAP.md`,
    `MemoraApp/gradle/libs.versions.toml`.
- **Documentation/traceability/ADR updates:** Registry + this record. No ADR-041.
- **Git commit:** Local checkpoint after verification (no push).
