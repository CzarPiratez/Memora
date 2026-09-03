# FC-02 Stage A — S3 latency device runbook

**Host:** Samsung SM-A156E  
**Bar:** pool-40 aggregate ≤ **800 ms**, or recorded
`DEGRADED_EXPLICIT` / `IDENTITY_FALLBACK` via `RecallRankLatencyPolicy`  
**Tag:** `MemoraRecallRankS3`  
**Does not:** wire Find; promote ORT; ship ADR-052 UI; claim AVAILABLE

## Steps

1. Unlock phone; USB debugging; select **SM-A156E**.
2. Open Logcat; filter **`MemoraRecallRankS3`**.
3. Run class:
   `com.memora.app.domain.intelligence.RecallRankStageALatencyIntegrationTest`
4. Paste lines containing `latency label=` and `disposition=`.

## How to read results

| Log field | Meaning |
|---|---|
| `full40_len128 wallMs` | S3 primary measurement |
| `reduced20_len96 wallMs` | Candidate degraded pool |
| `disposition=WITHIN_BUDGET` | Full pool OK under 800 ms |
| `disposition=DEGRADED_EXPLICIT` | Use effective pool 20; disclose slower/degraded posture |
| `disposition=IDENTITY_FALLBACK` | Skip CE on this host class until faster pack |

## Explicit non-claims

Not AVAILABLE. Not Stage A product wire. Not Stage B.
