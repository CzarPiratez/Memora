# Security

## Scope of this pack

This repository folder contains **documentation and contracts only**
(Apache-2.0). It must not include:

- API keys, tokens, or provider secrets
- Keystores, signing material, or passwords
- `local.properties` or machine-local paths
- Personal evaluation fixtures or private corpora
- AI Pack weights or proprietary pack configs
- Full application source from the private monorepo

If you find a secret or personal data in a published Class A tree, treat it as an
incident: do not reshare the material; report it (below).

## Reporting a vulnerability or sensitive leak

**Public issue tracker URL:** https://github.com/CzarPiratez/unfynd-core/issues  
(Use the public GitHub issues URL once the Class A-only public repository exists.)

Until that URL is published:

1. Prefer a private channel to the product owner (website contact at
   [https://www.unfynd.com/](https://www.unfynd.com/) or the copyright contact in
   `NOTICE`).
2. Do **not** paste secrets, keystores, or personal data into public issues or
   pull requests.
3. Include enough detail to reproduce or locate the issue without attaching
   private user content.

## Expectations

- Reports about **documentation accuracy** (over-claims, missing non-claims) are
  welcome once the public tracker exists.
- Reports about the **private Android app** or monorepo are out of scope for this
  pack’s tracker unless the publisher explicitly expands scope later.
