#!/usr/bin/env bash
# Domain layer purity guard (Phase 1 — Core / App separation)
#
# Authority: docs/CORE_APP_SEPARATION_PLAN.md Phase 1 step 1.4;
# docs/CHANGE_CONTROL_CORE_APP_PHASE1_BOUNDARY_HYGIENE.md
#
# Rule: domain/** (main source) must not import android.*, com.memora.app.data.*,
# com.memora.app.ui.*, or com.memora.app.application.*.
#
# Run from repo root:
#   bash scripts/check-domain-layer-purity.sh
# Windows helper:
#   powershell -File scripts/check-domain-layer-purity.ps1
# ---------------------------------------------------------------------------

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

DOMAIN_MAIN="MemoraApp/app/src/main/java/com/memora/app/domain"

FORBIDDEN_PATTERNS=(
  '^import android\.'
  '^import com\.memora\.app\.data\.'
  '^import com\.memora\.app\.ui\.'
  '^import com\.memora\.app\.application\.'
)

failures=0

while IFS= read -r -d '' file; do
  rel="${file#${ROOT}/}"
  rel="${rel//\\//}"
  for pattern in "${FORBIDDEN_PATTERNS[@]}"; do
    if grep -qE "${pattern}" "${file}"; then
      echo "FAIL: domain imports forbidden layer (${pattern}): ${rel}"
      grep -nE "${pattern}" "${file}" || true
      failures=$((failures + 1))
      break
    fi
  done
done < <(find "${DOMAIN_MAIN}" -name '*.kt' -print0 2>/dev/null || true)

if ((failures > 0)); then
  echo ""
  echo "Domain layer purity guard: FAIL (${failures} issue(s))"
  exit 1
fi

file_count=$(find "${DOMAIN_MAIN}" -name '*.kt' 2>/dev/null | wc -l | tr -d ' ')
echo "Domain layer purity guard: PASS"
echo "  domain main source files checked: ${file_count}"
exit 0
