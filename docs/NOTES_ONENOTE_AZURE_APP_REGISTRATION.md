# OneNote connector — Microsoft Entra app registration (N2 gate)

**Change control:** `docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md`  
**Package name:** `com.memora.app`  
**Client type:** Public native / Android (no client secret)

Memora cannot complete live OneNote sign-in (N2b) until this registration exists.
The client ID is a **public** identifier (not a secret). Still: do not commit a
production client ID unless the team intentionally wants that ID in the repo.
Prefer `local.properties` for developer builds.

## Create the registration

1. Open [Microsoft Entra admin center](https://entra.microsoft.com) → **App
   registrations** → **New registration**.
2. Name: e.g. `Memora OneNote (dev)`.
3. Supported account types: accounts in any org directory **and** personal
   Microsoft accounts (or narrower if you only test with work accounts).
4. Redirect URI: skip on create; add the **Android** platform next.
5. After create, copy **Application (client) ID**.

## Android platform + redirect

1. **Authentication** → **Add a platform** → **Android**.
2. Package name: `com.memora.app`.
3. Signature hash: generate from the **debug** keystore used by Android Studio
   (see portal instructions, or the `keytool` / MSAL helper hash for
   `~/.android/debug.keystore`, alias `androiddebugkey`, password `android`).
4. Save the redirect URI the portal shows (form
   `msauth://com.memora.app/<SIGNATURE_HASH>`).
5. Under **Authentication** → Advanced → **Allow public client flows** = **Yes**.

## API permissions (delegated only)

Add Microsoft Graph **delegated** permissions (admin consent only if your tenant
requires it for the chosen scopes):

| Scope | Why |
|-------|-----|
| `Notes.Read` | Read OneNote notebooks/pages (N3+) |
| `offline_access` | Refresh token so the user is not forced to sign in every session |
| `User.Read` | Minimal signed-in identity for MSAL account display (optional but common) |

Do **not** add `Notes.ReadWrite`, `Notes.Create`, or application (app-only)
permissions. OneNote Graph access for this product is **delegated + read-only**.

## Wire into a local Memora debug build

In `MemoraApp/local.properties` (gitignored), add:

```properties
memora.onenote.clientId=<Application client ID>
memora.onenote.signatureHash=<Android signature hash from the portal>
```

Rebuild the app. N2b reads these into `BuildConfig` and MSAL config. Empty values
keep the honesty UI in **registration required** and never claim Connected.

## What you send back to engineering

- Application (client) ID  
- Confirmation that Android package + signature hash are registered  
- Confirmation that only the scopes above were granted  

Do **not** create or share a client secret for this public Android client.
