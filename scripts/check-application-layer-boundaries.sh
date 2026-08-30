#!/usr/bin/env bash
# Application layer boundary guard (Phase 1 — Core / App separation)
#
# Authority: docs/CORE_APP_SEPARATION_PLAN.md Phase 1;
# docs/CHANGE_CONTROL_CORE_APP_PHASE1_BOUNDARY_HYGIENE.md
#
# Rule: application/** must not import com.memora.app.data.* except files on the
# shrink-only allowlist below. New violations fail CI.
#
# Run from repo root:
#   bash scripts/check-application-layer-boundaries.sh
# Windows helper:
#   powershell -File scripts/check-application-layer-boundaries.ps1
# ---------------------------------------------------------------------------

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

APP_MAIN="MemoraApp/app/src/main/java/com/memora/app/application"

# Shrink-only: remove a file when it no longer imports data.
ALLOW_APPLICATION_DATA_IMPORTS=(
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/IndexOneNotePages.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/OpenPersistedNotePageInOneNote.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/RunPendingOneNotePageExtract.kt"
)

is_allowlisted() {
  local rel="$1"
  for allowed in "${ALLOW_APPLICATION_DATA_IMPORTS[@]}"; do
    if [[ "${rel}" == "${allowed}" ]]; then
      return 0
    fi
  done
  return 1
}

failures=0
cleaned_allowlist=()
violations_in_allowlist=()

while IFS= read -r -d '' file; do
  rel="${file#${ROOT}/}"
  rel="${rel//\\//}"
  if ! grep -qE '^import com\.memora\.app\.data\.' "${file}"; then
    continue
  fi
  if is_allowlisted "${rel}"; then
    violations_in_allowlist+=("${rel}")
    continue
  fi
  echo "FAIL: application imports data (not allowlisted): ${rel}"
  grep -nE '^import com\.memora\.app\.data\.' "${file}" || true
  failures=$((failures + 1))
done < <(find "${APP_MAIN}" -name '*.kt' -print0 2>/dev/null || true)

# Allowlisted files must still violate (otherwise shrink the list).
for allowed in "${ALLOW_APPLICATION_DATA_IMPORTS[@]}"; do
  path="${ROOT}/${allowed}"
  if [[ ! -f "${path}" ]]; then
    echo "FAIL: allowlisted file missing (shrink list): ${allowed}"
    failures=$((failures + 1))
    continue
  fi
  if ! grep -qE '^import com\.memora\.app\.data\.' "${path}"; then
    cleaned_allowlist+=("${allowed}")
  fi
done

if ((${#cleaned_allowlist[@]} > 0)); then
  echo "FAIL: allowlist stale — these files no longer import data (remove from script):"
  printf '  %s\n' "${cleaned_allowlist[@]}"
  failures=$((failures + ${#cleaned_allowlist[@]}))
fi

allow_count=${#ALLOW_APPLICATION_DATA_IMPORTS[@]}
active_count=${#violations_in_allowlist[@]}

if ((failures > 0)); then
  echo ""
  echo "Application layer boundary guard: FAIL (${failures} issue(s))"
  exit 1
fi

echo "Application layer boundary guard: PASS"
echo "  allowlisted application→data files: ${active_count}/${allow_count} (shrink-only)"
exit 0
