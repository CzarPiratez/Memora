# FC-02 Stage A S4 — wire smoke runbook (device)

**Requirement:** Model brief S4 (identity when pack absent; meaning Find still works)  
**Change control:** `docs/CHANGE_CONTROL_FC02_STAGE_A_WIRE.md`  
**Does not authorize:** ADR-052 UI, AVAILABLE, Stage B, Core publish.

## Purpose

Prove on a physical device that Stage A CE is reachable from **product** meaning
Find when the ONNX file is present, and that **identity fallback** works when it
is absent — without crashing.

## Preconditions

| Item | Required |
|---|---|
| Device | Samsung SM-A156E (or equivalent midrange arm64) |
| App | Debug **app** install (not androidTest-only) |
| Meaning pack + index | USE installed; asset memories + meaning index built |
| CE weights | `no_backup/recall_rank_spike_staging/msmarco_minilm_l6_cross_encoder_qint8_v1.onnx` (~23 180 880 bytes) via S1 or adb push |

**Warning:** Android Studio **Run app** often reinstalls and **wipes** no-backup.
Prefer open-from-icon after staging the model.

## Checks

1. **CE present:** meaning Find a known cue; record top hit; confirm relevance.
2. **Identity:** rename `.onnx` → `.onnx.bak` via `adb` + `run-as`; force-stop;
   reopen; same Find; no crash.
3. **Restore:** rename `.bak` → `.onnx`; force-stop; reopen.

## Device evidence — Samsung SM-A156E (2026-09-04) — S4 PASS

```
CE present: query=silky top=spelling list 4 (1) -1.pdf containsWord=yes
Identity: model renamed away; no crash; top hit same spelling-list PDF
Restore: msmarco_minilm_l6_cross_encoder_qint8_v1.onnx bytes=23180880
Note: ranks 2–10 on single-token cue can be noisy (lexical AND needs ≥2 tokens)
```

## Explicit non-claims

- Not marketing AVAILABLE
- Not ADR-052 auto-install
- Not proof that single-token secondary ranking is solved
