# Fresh SAF Read-Grant Validation Boundary

**Status:** Implemented in code; emulator verification pending.  
**Requirements:** P-03, P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.

## Purpose

Android owns SAF authorization. Before Memora performs any operation that depends on
an approved document tree, it must freshly confirm that Android still retains the
exact tree's read grant. This is a reusable source-access gate, not a PDF parser or
document reader.

## Contract

`DocumentTreeAccessValidator` receives one private
`DocumentTreeApproval` and returns either:

- `GRANTED` only when Android's persisted grant list contains the exact stored
  tree URI with read permission; or
- `ACCESS_REVOKED` for every other case, including no matching grant, a different
  tree, or an exact URI without read permission.

The Android implementation reads only `ContentResolver.persistedUriPermissions`.
It must not query a DocumentsProvider, list a folder, open a document URI, open or
duplicate a descriptor, parse a PDF, persist an extraction, call a service, call
AI, or access the network.

Discovery asks this gate immediately before its metadata query. A future descriptor
opening adapter must ask the same gate immediately before it opens a document; it
must return the truthful revoked-access outcome without calling the parser if the
gate denies access.

## Pre-work record

- **Source documents read:** product registry, Local AI Technical Specification,
  governance, continuation record, product contract, architecture, decisions,
  roadmap, traceability, PDF extraction plan, parser-isolation review, and change
  control template.
- **Current-code evidence inspected:** the former grant check was embedded in
  `ContentResolverSafDocumentTreeCatalog` and consumed by
  `SafPdfDiscoverySource`. That would force a future extraction adapter to depend
  on a metadata-catalog abstraction. The catalog never opened a document.
- **Open ADRs / platform limitations checked:** ADR-003 remains unrelated; real
  source parsing remains disabled under ADR-017. A stored private approval alone is
  insufficient because Android can revoke the persisted read grant.
- **Privacy, source-access, dependency, offline, and data-retention impact:** no
  source content, provider metadata, descriptor, URI data beyond the existing
  private reference, Room record, AI, network, or dependency is added. The check is
  local and read-only.
- **Smallest safe change:** extract the platform grant check behind the
  source-neutral domain boundary, retain discovery behavior through that boundary,
  and add pure exact-match tests. Do not enable descriptor opening or parsing.
- **Acceptance criteria:** a different tree and write-only permission are denied;
  the exact read grant is accepted; a revoked result prevents discovery metadata
  queries; an existing approved folder still completes the one-page metadata-only
  emulator regression test.
- **Test and emulator verification plan:** run
  `PersistedDocumentTreeGrantMatcherTest` (3 unit tests), compile the debug Android
  test APK, then run `SafPdfDiscoverySourceIntegrationTest` (1 emulator test) with
  the already connected folder. The integration test must remain metadata-only.

## Delivery record

- **Files/layers changed:** domain access contract; Android SAF grant adapter;
  existing metadata discovery adapter/factory; dependency injection; focused unit
  tests; existing metadata-only emulator regression test.
- **Automated verification and result:** Kotlin, unit-test, and Android-test
  compilation passed on 2026-07-23. The focused
  `PersistedDocumentTreeGrantMatcherTest` result contains 3 passing tests: exact
  read-grant acceptance, different-tree rejection, and readless-grant rejection.
- **Emulator/manual verification and result:** On 2026-07-23, after explicitly
  reconnecting an emulator Documents folder, the user ran
  `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator:
  **1 test passed**. It used the new grant-validation boundary before one bounded
  metadata query.
- **Failure/recovery paths verified:** The source's existing revoked-access test
  confirms that a denied fresh grant produces `AccessRevoked`, makes no metadata
  query, and consults the validator for both the explicit state check and the
  discovery attempt.
- **Known limitation or follow-up:** this proves only a reusable fresh-grant gate.
  It does not open a document, enable real-source parsing, establish descriptor
  ownership, define persistence, or make a PDF searchable.
- **Documentation/traceability/ADR updates:** ADR-017, traceability, changelog, and
  continuation record updated in this checkpoint.
- **Git commit:** pending this documentation checkpoint.
