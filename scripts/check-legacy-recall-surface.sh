#!/usr/bin/env bash
# Legacy recall surface — machine guard (soft CI)
#
# Authority: docs/LEGACY_RECALL_SURFACE.md; docs/RECALL_ENFORCEMENT_INDEX.md;
# ADR-049 (Canonical Recall target-only); MIG-05 step 4 (page store retired);
# MIG-06 step 1 (additive SearchMemoryEvidence application use case allowed).
#
# Does NOT authorize MIG-07 cutover or Canonical Recall as a live product API.
# Run from repo root (Git Bash / WSL / GitHub Actions ubuntu):
#   bash scripts/check-legacy-recall-surface.sh
# Windows helper (forwards to this script):
#   powershell -File scripts/check-legacy-recall-surface.ps1
#
# Allowlisted L1–L4 keyword Find files (ui/** + application/** only).
# Any NEW *KeywordSearch* or SearchPersisted* file under those trees fails.
# SearchMemoryEvidence is ALLOWED only as the MIG-06 application use case
# (and its support/port files); still FORBIDDEN under ui/** or as a Find clone.
# ---------------------------------------------------------------------------

set -euo pipefail
shopt -s globstar nullglob

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

MAIN="MemoraApp/app/src/main"
MAIN_JAVA="${MAIN}/java"
MIGRATIONS_REL="MemoraApp/app/src/main/java/com/memora/app/data/local/MemoraDatabaseMigrations.kt"

# MIG-06 step 1: additive application use case + port + Room adapter (not UI Find).
ALLOW_SEARCH_MEMORY_EVIDENCE_FILES=(
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/SearchMemoryEvidence.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/MemoryEvidenceExcerptSearch.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/MemoryEvidenceLiteralSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/data/local/RoomMemoryEvidenceExcerptSearch.kt"
)

failures=0

fail() {
  echo "FAIL: $*" >&2
  failures=$((failures + 1))
}

ok() {
  echo "OK: $*"
}

if [[ ! -d "${MAIN}" ]]; then
  echo "ERROR: missing ${MAIN}" >&2
  exit 2
fi

# --- Allowlist: existing L1–L4 KeywordSearch / SearchPersisted surfaces -------
# Paths relative to repo root. Extend only with ADR + LEGACY_EXTENSION_EXCEPTION.
ALLOWLIST_KEYWORD_FIND=(
  # L1 PDF keyword
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/SearchPersistedPdfPageText.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/LoadPersistedPdfKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/PdfKeywordSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PdfKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PdfKeywordSearchCopy.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PdfKeywordSearchHighlight.kt"
  # L2 Screenshot OCR keyword
  "MemoraApp/app/src/main/java/com/memora/app/application/images/SearchPersistedScreenshotOcrText.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/LoadPersistedScreenshotOcrKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/ScreenshotOcrKeywordSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/ScreenshotOcrKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/ScreenshotOcrKeywordSearchScreen.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/ScreenshotOcrKeywordSearchCopy.kt"
  # L3 Photo OCR keyword
  "MemoraApp/app/src/main/java/com/memora/app/application/images/SearchPersistedPhotoOcrText.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/LoadPersistedPhotoOcrKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/PhotoOcrKeywordSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PhotoOcrKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PhotoOcrKeywordSearchScreen.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PhotoOcrKeywordSearchCopy.kt"
  # L4 Note keyword
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/SearchPersistedNotePageText.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/LoadPersistedNotePageKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/NotePageKeywordSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/NotePageKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/NotePageKeywordSearchScreen.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/NotePageKeywordSearchCopy.kt"
)

is_allowlisted() {
  local rel="$1"
  local a
  for a in "${ALLOWLIST_KEYWORD_FIND[@]}"; do
    if [[ "${rel}" == "${a}" ]]; then
      return 0
    fi
  done
  return 1
}

is_allowed_search_memory_evidence_file() {
  local rel="$1"
  local a
  for a in "${ALLOW_SEARCH_MEMORY_EVIDENCE_FILES[@]}"; do
    if [[ "${rel}" == "${a}" ]]; then
      return 0
    fi
  done
  return 1
}

# --- A: PdfPageEmbeddingStore / pdf_page_embeddings (migrations allowlisted) --
echo "== A: retired page-embedding substrate =="
a_failed=0
a_tmp="$(mktemp)"
grep -RIn --include='*.kt' --include='*.java' --include='*.xml' \
  -E 'PdfPageEmbeddingStore|pdf_page_embeddings' "${MAIN}" >"${a_tmp}" 2>/dev/null || true
while IFS= read -r line || [[ -n "${line}" ]]; do
  [[ -z "${line}" ]] && continue
  file="${line%%:*}"
  rel="${file#./}"
  rel="${rel//\\//}"
  if [[ "${rel}" == "${MIGRATIONS_REL}" ]]; then
    continue
  fi
  fail "forbidden PdfPageEmbeddingStore/pdf_page_embeddings in ${rel}"
  echo "  ${line}" >&2
  a_failed=1
done < "${a_tmp}"
rm -f "${a_tmp}"
if [[ "${a_failed}" -eq 0 ]]; then
  ok "no forbidden page-embedding tokens outside migrations"
fi

# --- B: no new KeywordSearch / SearchPersisted product Find clones ------------
echo "== B: no new *KeywordSearch* / SearchPersisted* under ui|application =="
b_failed=0
for file in \
  "${MAIN_JAVA}/com/memora/app/ui"/**/*.kt \
  "${MAIN_JAVA}/com/memora/app/application"/**/*.kt
do
  [[ -f "${file}" ]] || continue
  base="$(basename "${file}")"
  case "${base}" in
    *KeywordSearch*|SearchPersisted*)
      rel="${file#./}"
      rel="${rel//\\//}"
      if ! is_allowlisted "${rel}"; then
        fail "new/unallowlisted keyword Find file: ${rel}"
        b_failed=1
      fi
      ;;
  esac
done
if [[ "${b_failed}" -eq 0 ]]; then
  ok "all KeywordSearch/SearchPersisted files are allowlisted (L1–L4)"
fi

# --- C: CanonicalRecall type name must not exist in main yet ------------------
echo "== C: CanonicalRecall not in main (ADR-049 target-only) =="
if grep -RIn --include='*.kt' --include='*.java' -E '\bCanonicalRecall\b' "${MAIN}" >/dev/null 2>&1; then
  fail "CanonicalRecall appears in main (target name only until MIG-07)"
  grep -RIn --include='*.kt' --include='*.java' -E '\bCanonicalRecall\b' "${MAIN}" >&2 || true
else
  ok "no CanonicalRecall in main"
fi

# --- D: SearchMemoryEvidence — MIG-06 application allowlist; forbid UI Find ---
echo "== D: SearchMemoryEvidence MIG-06 application allowlist (not UI Find) =="
d_failed=0
d_tmp="$(mktemp)"
grep -RIn --include='*.kt' --include='*.java' -E '\bSearchMemoryEvidence\b' "${MAIN}" \
  >"${d_tmp}" 2>/dev/null || true
while IFS= read -r line || [[ -n "${line}" ]]; do
  [[ -z "${line}" ]] && continue
  file="${line%%:*}"
  rel="${file#./}"
  rel="${rel//\\//}"
  # Forbidden: any UI Find clone / ViewModel binding of SearchMemoryEvidence.
  if [[ "${rel}" == *"/ui/"* ]]; then
    fail "SearchMemoryEvidence under ui/ (MIG-07 cutover not authorized): ${rel}"
    echo "  ${line}" >&2
    d_failed=1
    continue
  fi
  # Mentions in PersistenceModule / MemoryDao KDoc are OK (not a new Find type).
  if [[ "${rel}" == "MemoraApp/app/src/main/java/com/memora/app/data/di/PersistenceModule.kt" ]] ||
     [[ "${rel}" == "MemoraApp/app/src/main/java/com/memora/app/data/local/MemoryDao.kt" ]] ||
     [[ "${rel}" == "MemoraApp/app/src/main/java/com/memora/app/data/local/RoomMemoryEvidenceExcerptSearch.kt" ]]; then
    continue
  fi
  if ! is_allowed_search_memory_evidence_file "${rel}"; then
    fail "SearchMemoryEvidence outside MIG-06 allowlist: ${rel}"
    echo "  ${line}" >&2
    d_failed=1
  fi
done < "${d_tmp}"
rm -f "${d_tmp}"

# Also forbid inventing a UI/ViewModel Find clone named *SearchMemoryEvidence*
for file in "${MAIN_JAVA}/com/memora/app/ui"/**/*.kt; do
  [[ -f "${file}" ]] || continue
  base="$(basename "${file}")"
  if [[ "${base}" == *SearchMemoryEvidence* ]]; then
    rel="${file#./}"
    rel="${rel//\\//}"
    fail "forbidden SearchMemoryEvidence UI/Find clone file: ${rel}"
    d_failed=1
  fi
done

if [[ "${d_failed}" -eq 0 ]]; then
  ok "SearchMemoryEvidence confined to MIG-06 application allowlist (not UI Find)"
fi

echo
if [[ "${failures}" -gt 0 ]]; then
  echo "legacy-recall-surface: ${failures} failure(s)" >&2
  exit 1
fi
echo "legacy-recall-surface: PASS"
exit 0
