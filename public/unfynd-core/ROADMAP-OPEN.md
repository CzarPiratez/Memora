# Openness roadmap (Class A pack)

This note separates what **this pack** opens now from what may open later or stay
closed. It is product strategy documentation, not a shipping schedule.

---

## Open now (Release Class A)

Curated **Public Specification / Contract** materials under Apache-2.0 in this
folder:

- Core definition and local-first rules
- Memory / evidence model and evidence classes
- Capability seams (including MemoryBuilder)
- Truth-before-intelligence / retrieval-first principles
- Staged direction and explicit non-claims
- Security reporting placeholder and notices

Public materials may say UNFYND Core is open **in the Class A sense**
(contracts/specs inspectable under Apache-2.0) with an explicit scope boundary.

---

## May open later (needs a new decision)

Examples that are **not** authorized by Class A alone:

| Candidate | Notes |
|---|---|
| Additional public SDK / interface docs | Only if curated and provenance-cleared |
| Selected non-secret reference samples | Must not leak corpora, keys, or private fixtures |
| Broader Core source under a separate license decision | Requires Model A vs B / licensing ADR |
| Website “open Core” wording beyond Class A | Must keep App vs Core boundary honest |

Exact file lists and licenses for any expansion need an explicit later ADR.

---

## Stays closed until a later ADR (typical)

- UNFYND Android reference application
- Private monorepo (may still use historical folder names)
- AI Pack weights, proprietary configs, and pack delivery secrets
- Proprietary tuning and evaluation corpora
- Secrets, keystores, credentials, `local.properties`, personal fixtures
- Commercial / enterprise technology packages

---

## Release Class B (later)

**Release Class B** — Commercial / Enterprise Technology Release — is a
**different** class: full readiness gates (architecture, IP, licensing, security,
quality, commercial) for redistributable technology packages, enterprise SDKs,
OEM licensing, and similar.

Class B is **not** done by this pack. Class A must not be read as permission to
commercially redistribute components that need a separate commercial license.

Any Class B (or broader Core source) publish requires a **new ADR** and clear
artifact boundaries.

---

## Publish mechanics (out of band)

Landing files under `public/unfynd-core/` in the private monorepo is **not** the
same as creating a public GitHub repository. Publishing **only** this pack to a
separate public remote is a later step: copy or subtree this folder, verify no
secrets, then push that public repo alone — never the private app tree.
