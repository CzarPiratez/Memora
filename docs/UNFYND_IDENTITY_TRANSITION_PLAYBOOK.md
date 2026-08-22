# UNFYND identity transition playbook

**Status:** Operating procedure (not architecture freeze)  
**Product identity:** UNFYND (formerly Memora)  
**Direction:** Personal Knowledge Infrastructure  
**Android app:** One implementation milestone / reference application  
**Rule:** One verified step at a time. Stop and wait for user feedback before the next step.

This playbook governs the **product-identity** transition and a **branding overlay**
on accepted frozen architecture. It does **not** authorize Event Memory, Knowledge
Memory, agents, grounded-answer implementation, package rename, or database rename.

## 1. Distinguished-engineer rules

1. Identity ≠ architecture ≠ domain language ≠ persistence.
2. Frozen architecture (invariants, pipelines, gates) stays frozen. Only the
   **product noun** on living canon may change, via overlay + ADR.
3. `Memory`, `MemoryEvidence`, `MemoryAnchor`, `Asset`, Find, Evidence Package,
   Memory Builder remain technical names unless a later ADR says otherwise.
4. `com.memora.app`, `memora.db`, Keystore aliases, MSAL redirect host, Room
   tables, and GitHub repo name are **out of scope** until an explicit later ADR.
5. Immutable `docs/product-source/Memora.docx` (and addenda) are **historical
   baselines**. Do not edit them in place. Register new UNFYND sources with SHA-256.
6. Accepted ADRs keep original text. Naming is superseded by the identity ADR,
   not by rewriting history.
7. No repository-wide find-and-replace of `Memora`.
8. No implementation of future PKI stages as part of this program.
9. Each step: governance pre-work, smallest change, acceptance criteria,
   verification, change-control, local git checkpoint (no push unless asked).
10. If a step would touch package, DB files, Entra, or GitHub rename — **stop**.

## 2. Classification (use on every edit)

| If the occurrence is… | Action |
|---|---|
| User-facing product name | UNFYND |
| Living canon subject (“Memora is retrieval-first”) | UNFYND |
| Historical ADR / changelog / change-control as shipped | PRESERVE; annotate if needed |
| `Memora.docx` filename / registry historical row | PRESERVE |
| `Memory*` / `Asset*` / evidence types / table names | KEEP |
| `com.memora.app` / `memora.db` / Keystore / MSAL host | KEEP (this program) |
| New UNFYND architecture files not yet in repo | REGISTER as new artifacts |

## 3. Working agreement with the user

- Operator issues **one prompt per step**.
- User (or Agent) executes **only that step**.
- User returns: what landed, verification, issues, decisions.
- Next prompt is written only after that feedback.
- Do not “helpfully” start the following step.

## 4. Steps (strict order)

### Step 0 — Land this playbook (docs only)

- Add this file. Link from CONTINUE “read in this order” only if the user
  accepts that in this step’s prompt (default: playbook file only).
- **Out:** no identity ADR yet, no string changes, no architecture rewrite.

**Acceptance:** File exists; no app/package/DB change.

### Step 1 — Identity ADR + registry interpretation (docs only)

- Record accepted decision: UNFYND is current product identity; Memora is former
  name; PKI is the north star; Android is a milestone; technical IDs unchanged.
- Update `PRODUCT_SOURCE_REGISTRY.md` interpretation: historical PRD preserved;
  current identity UNFYND; new architecture docs listed when hashed.
- Changelog + change-control for this ADR only.
- **Out:** no mass rewrite of constitutions; no UI.

**Acceptance:** ADR number assigned after last accepted ADR; registry does not
contradict UNFYND; `Memora.docx` hash unchanged.

### Step 2 — Inventory and register new architecture documents (docs only)

- List every new/frozen architecture file (in-repo and any user-provided).
- Classify: canon vs amendment vs draft vs duplicate.
- Register accepted files with SHA-256. Do not silently replace grounding
  constitution. If two constitutions conflict, **stop for product decision**.
- Update CONTINUE read-order for identity + PKI + grounding.
- **Out:** no product-noun overlay yet except registry/CONTINUE pointers.

**Acceptance:** Single list of governing docs; no competing unmarked constitutions.

### Step 3 — Overlay living constitutions (docs only, product noun only)

- Files such as: GOVERNANCE, PRODUCT_CONTRACT, ARCHITECTURE, AGENTS,
  EXPERIENCE_MEMORY_AMENDMENT, GROUNDED_ANSWERS_AMENDMENT, GROUNDING_ARCHITECTURE,
  LOCAL_AI_TECHNICAL_SPEC (identity sentences only), README vision.
- Add a short identity header on constitutions: UNFYND (formerly Memora); PKI;
  freeze remains the technical invariants.
- Mechanical pass: product-as-subject only. Do not rename Memory types.
- Do not rewrite ADR-018/019/033–039 bodies; identity ADR supersedes naming.
- **Out:** no `strings.xml`, no Kotlin copy.

**Acceptance:** Spot-check: lead invariants say UNFYND; Memory/Find/evidence
unchanged; historical ADRs intact.

### Step 4 — User-visible Android brand (copy only)

- `app_name` and related strings; copy objects; copy unit tests; notices header.
- **Out:** applicationId, namespace, MemoraDatabase, memora.db, MSAL package.

**Acceptance:** Unit copy tests green; user confirms launcher + Clear index +
unlock + Welcome/Notes/Find copy on emulator.

### Step 5 — Optional internal names (only if requested)

- Theme/Application/composable names. Prefer **not** renaming MemoraDatabase.
- Separate checkpoint. Still no package/DB.

### Explicitly deferred (own ADRs later)

- `applicationId` / namespace / Entra MSAL
- `memora.db` and Keystore aliases
- GitHub repository name / `MemoraApp/` folder
- Grounded Answers code, Event/Knowledge Memory, agents

## 5. Step completion template (user returns this)

- Step number:
- Files changed:
- Verification run and result:
- Anything left Memora that should have been UNFYND (product noun only):
- Anything almost renamed that must stay (Memory / package / DB):
- Decision needed before next step: yes/no — if yes, what:
- Ready for next step: yes/no

## 6. Hard stop conditions

Stop the program and ask the user if the next edit would:

- change `applicationId`, namespace, or MSAL redirect host;
- rename database files or Keystore aliases;
- rewrite `Memora.docx`;
- implement generative reasoning or new memory stages;
- register two conflicting constitutions without a decision.
