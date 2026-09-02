# A-01 offline end-to-end proof — device runbook

**Requirement:** A-01 (`docs/LOCAL_AI_BENCHMARK_PLAN.md`, `docs/ROADMAP.md`)  
**Authority:** Closes MVP exit audit row when recorded in change control.  
**Does not authorize:** marketing AVAILABLE (separate founder decision).

## Purpose

Prove on a **physical Android device** that after the on-device AI pack is installed,
the **core path** works with **no network**:

```text
indexed corpus → keyword Find → meaning Find → Why (Explain)
```

without remote inference or Memora cloud sync.

## Preconditions

| Item | Required |
|---|---|
| Physical device | arm64 phone (midrange preferred — e.g. Galaxy A15 class) |
| UNFYND debug or release build | Latest build with meaning-search anchor fix |
| AI Pack | Universal Sentence Encoder (USE) **installed** on device |
| Test corpus | Known PDFs/photos on device (e.g. MemoraFixtures or your real library) |
| Meaning index | Built at least once while online (pack download) |

Record: device model, Android version, build git SHA, date (UTC).

## Setup (online once)

1. Install the app build on the device.
2. Grant media / PDF / notes permissions as needed.
3. Complete AI Pack install (USE) — wait until disclosure shows pack **available**.
4. Run **Build Asset Memories** for a small known set (≥2 PDFs or mixed types).
5. Tap **Build meaning index** until corpus honesty shows indexed vectors > 0.
6. **Smoke online:** keyword Find + meaning Find on a known cue (e.g. `invoice`,
   `mira`) — confirm results before airplane mode.

## Offline proof (A-01)

1. Enable **Airplane mode** (Wi‑Fi and mobile data off). Confirm no network icon.
2. Force-stop the app; reopen (cold start).
3. **Keyword Find** — search a cue that worked online; open a hit; open **Why**.
4. **Meaning Find** — same or second cue; confirm results (not “could not finish”).
5. **Open original** — from a hit, open cited PDF page or asset if offered.
6. Optional: toggle airplane off → confirm app still healthy (no corruption).

## Pass criteria

| Step | Pass |
|---|---|
| Keyword Find returns hits offline | Yes / No |
| Meaning Find returns hits offline | Yes / No |
| Why shows stored evidence (not invented) | Yes / No |
| Open original works for at least one hit | Yes / No |
| No crash / ANR during steps 2–5 | Yes / No |

**A-01 PASS** only if all five are **Yes**.

## Fail handling

| Symptom | Likely cause | Action |
|---|---|---|
| Meaning “could not finish” | Index/model not loaded offline | Rebuild meaning index online; retest |
| No meaning hits | Empty index or wrong model | Check corpus honesty counts |
| Keyword works, meaning empty | Embedding store missing | Reinstall pack; rebuild index |
| Crash on Why | Regression | File bug; do not claim A-01 |

## Evidence to capture (for change control)

- Device model + Android version
- Build identifier (git SHA)
- Screenshots: airplane mode, Find results, Why screen, corpus honesty counts
- Cues tested (keyword + meaning)
- Pass/fail table above
- Any degraded copy shown (candidate / not measured AVAILABLE)

## Recording the result

When complete, open or update:

`docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md`

Use the delivery record template from `docs/CHANGE_CONTROL_TEMPLATE.md`.
Update `docs/MVP_EXIT_AUDIT.md` row **A-01** from PARTIAL/FAIL to PASS with
pointer to that change control.

## Related

- Meaning Find on device post anchor fix: repeat steps 3–4 offline; update audit
  row “Find by meaning — physical device E2E”.
- `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` — AVAILABLE still
  separate after A-01.
