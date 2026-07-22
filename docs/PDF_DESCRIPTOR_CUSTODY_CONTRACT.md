# Approved PDF Descriptor Custody Contract

**Status:** Implemented and verified as a pure-domain gate.
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.

## Purpose

The future ordinary-process PDF source broker needs an explicit decision before it
opens a descriptor for a user-approved document. The decision must bind the
extraction request to the exact approved source and a freshly observed access state,
without carrying a content URI, descriptor, stream, parser, or Android framework
type inward.

## Contract

`ApprovedPdfDescriptorCustodyContract` accepts:

- a validated `PdfExtractionRequest` (therefore a PDF Asset);
- the exact private `DocumentTreeApproval` selected by the source repository; and
- the just-observed `SourceAccessState` from `DocumentTreeAccessValidator`.

It returns:

- `Authorized` only if the request Asset's source ID equals the approval source ID
  and the fresh access state is `GRANTED`. The result retains only immutable Asset
  identity and fingerprint.
- `SourceMismatch`, `AccessRequired`, `AccessRevoked`, or
  `SourceUnavailable` otherwise. These results contain no URI, document name,
  parser data, or source content.

The future Android broker must perform these actions in order:

1. load the exact approval for the request Asset's source ID;
2. freshly validate the Android persisted read grant;
3. call this pure contract;
4. if and only if it is authorized, prove the Asset location belongs to the approved
   Android tree and open one read-only descriptor;
5. duplicate only that descriptor to the isolated service, then close ordinary and
   service-owned descriptor handles on every path.

This contract does not implement steps 4 or 5.

## Pre-work record

- **Source documents read:** product registry, Local AI Technical Specification,
  governance, continuation record, product contract, architecture, decisions,
  roadmap, traceability, PDF extraction plan, parser-isolation review, fresh-grant
  validation plan, and change-control template.
- **Current-code evidence inspected:** `PdfExtractionRequest` already requires a
  PDF Asset and `DocumentTreeAccessValidator` now exposes the fresh persisted-grant
  state. The current isolated parser client accepts an already-opened synthetic
  descriptor and deliberately has no source identity, URI, or grant API.
- **Open ADRs / platform limitations checked:** ADR-017 still prohibits real-source
  parsing. The domain cannot prove Android URI tree membership or close an Android
  descriptor; those remain future platform-broker responsibilities.
- **Privacy, source-access, dependency, offline, and data-retention impact:** this
  pure change has no Android, descriptor, stream, source I/O, persistence, parser,
  service, UI, AI, network, or new dependency.
- **Smallest safe change:** introduce a content-free decision model and unit tests
  for the matching-source, mismatch, revoked, required, and unavailable outcomes.
- **Acceptance criteria:** authorization retains only immutable identity/fingerprint;
  every non-granted or mismatched state denies authorization; no platform type is
  imported by the domain contract.
- **Test and emulator verification plan:** run
  `ApprovedPdfDescriptorCustodyContractTest` in Android Studio. It should report
  **5 tests passed**. Because this is pure domain code with no Android operation,
  no source, descriptor, or emulator asset is accessed.

## Delivery record

- **Files/layers changed:** `ApprovedPdfDescriptorCustodyContract` in the domain
  layer, its five pure unit tests, and this contract record. No platform adapter,
  parser client/service, source repository, Room record, UI, worker, or dependency
  changed.
- **Automated verification and result:** on 2026-07-23, focused local Gradle
  verification passed `ApprovedPdfDescriptorCustodyContractTest`: **5 tests passed**.
- **Emulator/manual verification and result:** on 2026-07-23, the user ran the same
  test in Android Studio and confirmed **5 tests passed**. This is a pure JVM/domain
  test: it accesses no emulator asset, Android source, descriptor, or document.
- **Failure/recovery paths verified:** source mismatch, access required, access
  revoked, and source unavailable all deny authorization without exposing source
  location or content; the future platform adapter remains responsible for presenting
  a truthful retry/reconnect action.
- **Known limitation or follow-up:** an approved descriptor opening adapter, Android
  tree-membership proof, descriptor ownership implementation, bounded real-source
  parser protocol, persistence, resource policy, and user-visible recovery remain
  separate ADR-017 gates.
- **Documentation/traceability/ADR updates:** this delivery record, continuation
  record, change log, and P-07 traceability evidence are updated. No new ADR is
  required: ADR-017 already governs the still-disabled real-source parsing path.
- **Git commit:** recorded with this verified delivery step.
