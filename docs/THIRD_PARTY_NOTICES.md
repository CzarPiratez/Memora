# Third-party notices

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
