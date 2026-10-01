# Third-party notices

## Notes connector auth (N2b)

| Component | Exact version | Purpose | Licence / notice source |
|---|---:|---|---|
| `com.microsoft.identity.client:msal` | 8.4.1 | Microsoft sign-in for read-only OneNote connector | MIT; [MSAL Android](https://github.com/AzureAD/microsoft-authentication-library-for-android) |

Review record: `docs/dependency-review/msal-android-8.4.1-review.md`.
INTERNET is for this source-access path only — not Local-AI and not Memora cloud sync.

## Screenshot OCR (Phase 2 deterministic extract)

| Component | Exact version | Purpose | Licence / notice source |
|---|---:|---|---|
| `com.google.mlkit:text-recognition` | 16.0.1 | Bundled Latin on-device OCR for catalogued screenshots | [ML Kit terms / notices](https://developers.google.com/ml-kit/terms); Maven Central artifact |

Review record: `docs/dependency-review/mlkit-text-recognition-16.0.1-review.md`.
This does not authorize network access, AI Pack download, or PHOTO OCR.

## On-device meaning embeddings (ADR-055 slice 4 product pack)

| Component | Exact version | Purpose | Licence / notice source |
|---|---:|---|---|
| Xenova/bge-small-en-v1.5 `onnx/model_quantized.onnx` | quantized-v1 (sha256 `6c9c6101…`) | Product on-device meaning embedder after bake-off win | MIT ([BAAI/bge-small-en-v1.5](https://huggingface.co/BAAI/bge-small-en-v1.5)); ONNX export via Xenova |
| `com.microsoft.onnxruntime:onnxruntime-android` | 1.28.0 | Already on classpath for Stage A; product BGE inference | MIT; see ONNX Runtime review |

Download is **model bytes only** after disclosure. Does not upload Memories.
Replaces MediaPipe USE as the product meaning pack (ADR-055 slice 4).
Rebuild the meaning index after install; USE vectors are purged so packs
never mix.

## Retired MediaPipe USE embedder (E5b1 / ADR-031–032; purge-only)

| Component | Exact version | Purpose | Licence / notice source |
|---|---:|---|---|
| `com.google.mediapipe:tasks-text` | 0.10.29 | Kept for historical measurement baselines; not the product pack | Apache License 2.0; [MediaPipe](https://developers.google.com/mediapipe) / Maven Central |

Review record: `docs/dependency-review/mediapipe-tasks-text-0.10.29-review.md`.

## Meaning encoder bake-off challenger store (ADR-055 slice 3 archive)

Debug About may still download a second private BGE copy for probe-only
re-score against the frozen USE card. That path does not write Room and
does not change Find. Product pack is the private store above.

## PDF extraction validation dependency set

This inventory applies to the synthetic-fixture validation step only. It does not
authorize opening a user document, copying source content, or network access at
runtime.

| Component | Exact version | Purpose | Licence / notice source |
|---|---:|---|---|
| `com.tom-roush:pdfbox-android` | 2.0.27.0 | Local PDF metadata and text parser behind a data-layer boundary | Apache License 2.0; [project repository](https://github.com/tomroush/pdfbox-android) and [Maven Central record](https://central.sonatype.com/artifact/com.tom-roush/pdfbox-android) |
| `org.bouncycastle:bcprov-jdk15to18` | 1.84 | Reviewed, pinned replacement for the parser's transitive cryptography provider | [Bouncy Castle licence](https://www.bouncycastle.org/licence.html) |
| `org.bouncycastle:bcpkix-jdk15to18` | 1.84 | Reviewed, pinned replacement for the parser's transitive PKIX support | [Bouncy Castle licence](https://www.bouncycastle.org/licence.html) |
| `org.bouncycastle:bcutil-jdk15to18` | 1.84 | Reviewed, pinned replacement for the parser's transitive utility support | [Bouncy Castle licence](https://www.bouncycastle.org/licence.html) |

The project must retain the applicable licence text and attribution from every resolved
artifact in the release distribution. The versioned component list and OSV result live
in `docs/dependency-review/`; they must be regenerated for every dependency upgrade.
