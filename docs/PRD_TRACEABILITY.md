# PRD Traceability Matrix

This is the implementation checklist derived from `Memora.docx`. It is intentionally
product-facing: a feature is not complete merely because code exists; it must satisfy
the behavior described here.

| ID | PRD requirement | Planned owner/layer | Verification evidence | Status |
|---|---|---|---|---|
| P-01 | Memora behaves as a memory retrieval engine, not conventional file search | Product, recall UI | Query can be answered from recall cues rather than filename | Planned |
| P-02 | Lifecycle is Discover -> Extract -> Understand -> Store -> Recall -> Explain | Application architecture | End-to-end test traces one asset through every stage | Planned |
| P-03 | Photos, screenshots, PDFs, and notes are MVP sources | Source adapters | Each approved source produces Asset candidates | Planned |
| P-04 | Discovery is continuous/incremental and source assets get stable internal identity | Source adapters, Room, WorkManager | Restart/change tests show no duplicates and changed assets requeue | Planned |
| P-05 | Discovery knows source facts before it knows semantic content | Domain | Placeholder Asset records contain identity/type/time/location only | Planned |
| P-06 | Images expose deterministic metadata, including OCR and available EXIF/GPS | Extraction | Image fixture tests | Planned |
| P-07 | PDFs expose full text, page count, title, and metadata when available | Extraction | PDF fixture tests | Planned |
| P-08 | Notes expose raw text and permitted source metadata | Note adapter/extraction | Provider fixture tests | Blocked by ADR-003 |
| P-09 | Assets are normalized into a common Memory structure | Domain, Room | Cross-source schema tests | Planned |
| P-10 | Semantic understanding creates memories/signatures/anchors/summary | Understanding service | Validated structured-output fixtures | Planned |
| P-11 | The system invests intelligence during indexing, not by repeatedly re-reading files during search | Repository, recall | Search test works from stored memory data | Planned |
| P-12 | Natural-language recall uses evidence-based ranking | Recall engine | Query ranking tests | Planned |
| P-13 | Explain Mode states why a result matched | Recall UI | Explanation references stored evidence fields | Planned |
| P-14 | Android work is offline-first, recoverable, and background-safe | WorkManager, Room | Interrupted/retry tests | Planned |
| P-15 | Original files are not edited or deleted | All source adapters | Read-only permission and integration review | Planned |
| P-16 | UI is recognition-first, not a dashboard of technical filters | Compose UI | User review against query/result flows | Planned |
| P-17 | MediaStore, Room, WorkManager, Compose, MVVM, repository pattern, and Hilt form the Android foundation | Platform/data/app layers | Architecture review and build | Planned |
| P-18 | WhatsApp, Gmail, Calendar, video, audio, timeline, cloud sync, collaboration, manual tags, folders, and phone-wide chat remain out of MVP | Scope control | PR review and roadmap check | Accepted |
| P-19 | User accounts are excluded, yet existing notes must be indexed | Product decision | ADR-003 resolved before note connector work | Open conflict |

## Definition of traceable delivery

Before closing any feature, link its tests and visible behavior to at least one ID in
this table. If a proposed feature has no matching requirement, either decline it as
out of scope or record an explicit product decision before implementation.
