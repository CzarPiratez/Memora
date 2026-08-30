# Openness roadmap (Class A pack)

This note separates what **this pack** opens now from what arrives in later
openness phases. It is product strategy documentation, not a shipping schedule
for every vertical application in [`APPLICATIONS.md`](APPLICATIONS.md).

---

## Open now (Release Class A)

Curated **Public Specification / Contract** materials under Apache-2.0 in this
repository:

- Core vision, local-first rules, and builder model ([`SPEC.md`](SPEC.md))
- Capability and industry applications ([`APPLICATIONS.md`](APPLICATIONS.md))
- Memory / evidence model and evidence classes
- Capability seams (including MemoryBuilder)
- Truth-before-intelligence / retrieval-first principles
- Staged Memory direction as future stages
- Synthetic illustrative examples ([`examples/`](examples/); ADR-048)
- Security reporting, notices, contributing, and short governance
- Pack changelog ([`PUBLIC_CHANGELOG.md`](PUBLIC_CHANGELOG.md))

Public materials may say UNFYND Core is open **in the Class A sense**
(contracts/specs inspectable under Apache-2.0) with an explicit scope boundary:
this pack is foundations you can read and build on; the App and full stack remain
under separate decisions.

---

## Later openness phases

As Core and this pack mature, further foundations may open under new decisions:

| Phase direction | Notes |
|---|---|
| Runnable tooling that exercises Class A contracts | Validator and related developer tooling as pack updates |
| Additional public SDK / interface docs | Only if curated and provenance-cleared |
| Broader Core source under a separate license decision | Requires Model A vs B / licensing ADR |
| Website and Open pages pointing at this pack | Keep App vs Core boundary clear |

**Reference samples:** Synthetic, non-personal Class A examples under the same
Apache-2.0 license as this pack are authorized (private monorepo ADR-048). They
must not leak corpora, keys, or private App fixtures. Further sample expansions
stay curated.

Exact file lists and licenses for any expansion beyond that need an explicit
later ADR where required.

---

## Stays in private trees until a later ADR

- UNFYND App
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

Class B is a later phase relative to this pack. Commercial product and
enterprise technology licensing is **separate** from this Apache-2.0 contracts
pack. Class A must not be read as permission to commercially redistribute
components that need a separate commercial license.

Any Class B (or broader Core source) publish requires a **new ADR** and clear
artifact boundaries.

---

## Publish mechanics (out of band)

Landing files under `public/unfynd-core/` in the private monorepo is **not** the
same as publishing this public repository. Publishing **only** this pack to a
separate public remote means: copy or subtree this folder, verify no secrets,
then push that public repo alone — never the private monorepo.
