# MSAL Android dependency review (Notes N2b)

**Status:** On classpath for N2b.  
**Date:** 2026-08-01  
**Artifact:** `com.microsoft.identity.client:msal:8.4.1`  
**Change control:** `docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md`

## Purpose

Acquire and refresh **delegated** Microsoft identity tokens for read-only OneNote
Graph access. Not a Memora account. Not Memora cloud sync.

## Alternatives considered

| Option | Why not chosen |
|---|---|
| Custom OAuth + AppAuth only | More surface area; MSAL is the supported Android path for Entra |
| Embedded WebView + client secret | Forbidden — public client; no secret in APK |
| Defer auth until N3 | Discovery cannot call Graph without a token vault + sign-in |

## Privacy / security

- Declares **INTERNET** and **ACCESS_NETWORK_STATE** for Microsoft sign-in / future
  Graph source access only — not Local-AI and not Memora cloud sync.
- Tokens mirrored into the N2a Keystore-backed vault; MSAL account cache cleared on
  Disconnect and Clear Memora index.
- Scopes: `Notes.Read`, `offline_access`, `User.Read` (delegated).
- Public client ID + signature hash from `local.properties` → `BuildConfig` only.
- Unit/resource checks assert no `client_secret` / refresh-token fixtures in
  packaged resources.

## Licence / notices

MIT License (Microsoft Identity library). Attribution in
`docs/THIRD_PARTY_NOTICES.md` and `MemoraApp/app/src/main/res/raw/open_source_notices.txt`.

## Follow-up

Emulator auth smoke on a throwaway Microsoft account before accepting N2b.
