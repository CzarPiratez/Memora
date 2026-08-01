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
