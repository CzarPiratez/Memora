# Architecture Decision Record

## ADR-001: Memora indexes sources; it is not an import inbox

**Status:** Accepted

**Decision:** The primary workflow is continuous, permissioned source discovery and
incremental indexing. Manual per-item sharing or uploading is not a substitute for a
source adapter.

**Reason:** This preserves the PRD's search-engine model and its Discover -> Extract
-> Understand lifecycle.

## ADR-002: Original source content remains read-only

**Status:** Accepted

**Decision:** Memora stores references and derived memory data. It never edits,
renames, moves, or deletes original assets.

**Reason:** Privacy and user trust are product requirements.

## ADR-003: Existing third-party notes versus no accounts/cloud sync

**Status:** Open - requires an explicit product decision

**Facts:**

- The MVP requires automatic indexing of Notes.
- The PRD excludes user accounts and cloud sync.
- Android cannot permit Memora to scan another app's private note database.

**Viable choices:**

1. Allow one narrowly scoped, read-only provider connector (recommended first:
   OneNote) and explicitly treat it as source authorization rather than Memora user
   accounts or write-back sync.
2. Narrow the MVP notes claim to note files in user-approved storage locations.
3. Remove automatic existing notes from the MVP; this conflicts with the current PRD
   and is not recommended.

**Rule:** Do not implement or advertise automatic indexing of arbitrary note apps until
this decision is accepted.

## ADR-004: Platform access is source-specific

**Status:** Accepted

**Decision:** Use MediaStore for images/screenshots, persisted Storage Access Framework
permissions for PDFs, and a provider adapter for external notes.

**Reason:** A single broad storage permission is neither sufficient nor appropriate on
modern Android.

## ADR-005: Room code generation uses Android legacy KAPT temporarily

**Status:** Accepted with review trigger

**Decision:** Use the `com.android.legacy-kapt` bridge for Room's compiler while this
project uses AGP built-in Kotlin.

**Reason:** The current KSP plugin attempts to configure generated Kotlin source sets
in a way that AGP built-in Kotlin rejects. Android's official migration guidance
provides legacy KAPT for this compatibility case. We will not suppress the AGP safety
check or disable built-in Kotlin merely to make KSP compile.

**Review trigger:** Re-evaluate KSP when a compatible plugin/toolchain combination is
available. This choice affects build-time code generation only; it does not affect the
app's runtime data or privacy model. While legacy KAPT is in use, the Room schema path
is passed explicitly to the annotation processor and the generated schema JSON remains
version-controlled.
