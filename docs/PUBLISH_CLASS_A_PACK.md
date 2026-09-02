# Publish Class A pack to public GitHub

**Authority:** `public/unfynd-core/ROADMAP-OPEN.md` publish mechanics  
**Does not publish:** the private Memora monorepo or UNFYND App source.

## What gets published

Only the contents of `public/unfynd-core/` — Apache-2.0 contracts, examples,
validator, `BUILDING.md`, planning sketches.

## Preconditions

- [ ] `python public/unfynd-core/tools/validate_class_a_examples.py --self-test` passes
- [ ] No secrets, keystores, private fixtures, or grant language in the pack
- [ ] `PUBLIC_CHANGELOG.md` updated for this revision

## Steps (manual — out of band)

1. Clone the public remote locally (or use a fresh directory):
   `git clone https://github.com/CzarPiratez/unfynd-core.git`
2. Copy updated files from this monorepo `public/unfynd-core/` into that clone
   (preserve `LICENSE`, `NOTICE`, structure).
3. Run validator in the clone: `python tools/validate_class_a_examples.py --self-test`
4. Commit with a pack revision message; push to `main` on the public remote only.
5. Verify GitHub Actions on the **public** repo if configured there, or rely on
   private monorepo CI `class-a-validator` job before copy.

## Never

- Push the private `Memora` monorepo to a public remote
- Include `MemoraApp/`, AI pack weights, or `docs/` private governance in the public repo

## After publish

- Optional: note pack revision date on unfynd.com/core (website — separate from this doc)
- Private `CONTINUE.md` may record publish SHA for traceability
