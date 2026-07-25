from pathlib import Path

p = Path(r"c:\Users\DELL\Documents\Memora\CONTINUE.md")
t = p.read_text(encoding="utf-8")
old = """## Next approved engineering step

Remaining ADR-017 gate before production PDF content / real-source use:

1. Real-source descriptor path.

Do not wire production discovery/UI to persist PDF text from real documents,
schedule WorkManager, invoke AI, or use the network until that gate passes."""
new = """## Verified engineering checkpoint

ADR-017 real-source descriptor path is verified. On 2026-07-25, fingerprint
revalidation (`ApprovedPdfFingerprintRevalidationContract` **3 of 3**) plus Medium
Phone emulator broker/parser suites (**11 of 11**) passed, including
`ParseApprovedPdfWithIsolatedParserRealSourceIntegrationTest`: create one PDF under
the user-approved SAF tree, open through the real broker with fresh-grant and
fingerprint checks, parse in the isolated service, return status only, delete the
fixture. No production UI wiring, Room extraction write from this path, WorkManager,
AI, or network was added.

## Next approved engineering step

ADR-017 isolation gates for synthetic + real-source descriptor opening are closed for
the current bar. Remaining before searchable PDF text in product:

1. Wire a governed production path that may persist validated extraction text (still
   behind explicit user action / truthful UI), without WorkManager/AI/network until
   separately approved.

Do not claim searchable PDF recall until that persistence/UI wiring is verified."""
if old not in t:
    raise SystemExit("CONTINUE next block missing")
p.write_text(t.replace(old, new), encoding="utf-8", newline="\n")
print("CONTINUE ok")

cl = Path(r"c:\Users\DELL\Documents\Memora\docs\CHANGELOG.md")
c = cl.read_text(encoding="utf-8")
insert = """## Unreleased

### Verified real-source SAF PDF descriptor path (status-only)

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Fingerprint revalidation before open; StaleSource outcomes; live
  approved-tree fixture open through broker + isolated parser returning status only.
- **Verification:** On 2026-07-25, unit 3 of 3; Medium Phone emulator 11 of 11
  (broker + synthetic parse + real-source parse).
- **Truthfulness:** No production UI parse wiring, searchable Room write from this
  path, WorkManager, AI, or network.

"""
if "### Verified real-source SAF PDF descriptor path" not in c:
    cl.write_text(c.replace("## Unreleased\n\n", insert, 1), encoding="utf-8", newline="\n")
    print("CHANGELOG ok")
