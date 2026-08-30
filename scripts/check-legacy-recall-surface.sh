#!/usr/bin/env bash
# Legacy recall surface — machine guard (soft CI)
#
# Authority: docs/LEGACY_RECALL_SURFACE.md; docs/RECALL_ENFORCEMENT_INDEX.md;
# ADR-049 (Canonical Recall target-only); MIG-05 step 4 (page store retired);
# MIG-06 SearchMemoryEvidence application use case;
# MIG-07 PDF + screenshot + photo + note keyword Find cutovers (L1–L4 Retired;
# L7 + L8 still Live).
#
# Does NOT authorize MIG-07B. Canonical Recall thin KEYWORD façade is allowed
# (application CanonicalRecall + four keyword ViewModels). SearchMemoryEvidence
# remains candidate generation (not UI-bound).
# Run from repo root (Git Bash / WSL / GitHub Actions ubuntu):
#   bash scripts/check-legacy-recall-surface.sh
# Windows helper (forwards to this script):
#   powershell -File scripts/check-legacy-recall-surface.ps1
#
# Allowlisted keyword Find UI/helper files for L1–L4 Retired MemoryEvidence-backed
# paths (ui/** + application/** only). Any NEW *KeywordSearch* or SearchPersisted*
# file under those trees fails.
# SearchMemoryEvidence is ALLOWED as MIG-06 candidate gen (+ support + adapters +
# readiness) and inside CanonicalRecall; FORBIDDEN under ui/**.
# CanonicalRecall is ALLOWED as application façade + four keyword ViewModels.
# ---------------------------------------------------------------------------

set -euo pipefail
shopt -s globstar nullglob

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

MAIN="MemoraApp/app/src/main"
MAIN_JAVA="${MAIN}/java"
MIGRATIONS_REL="MemoraApp/app/src/main/java/com/memora/app/data/local/MemoraDatabaseMigrations.kt"

# MIG-06 application use case + port + Room adapter; MIG-07 PDF + screenshot + photo + note adapters.
ALLOW_SEARCH_MEMORY_EVIDENCE_FILES=(
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/SearchMemoryEvidence.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/CanonicalRecall.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/MemoryEvidenceExcerptSearch.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/MemoryEvidenceLiteralSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/data/local/RoomMemoryEvidenceExcerptSearch.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/PdfMemoryEvidenceKeywordAdapter.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/PdfKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/LoadPersistedPdfKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/ScreenshotMemoryEvidenceKeywordAdapter.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/ScreenshotOcrKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/LoadPersistedScreenshotOcrKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/PhotoMemoryEvidenceKeywordAdapter.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/PhotoOcrKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/LoadPersistedPhotoOcrKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/NoteMemoryEvidenceKeywordAdapter.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/NotePageKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/LoadPersistedNotePageKeywordSearchReadiness.kt"
)

# Keyword ViewModels bind CanonicalRecall (not SearchMemoryEvidence).
ALLOW_CANONICAL_RECALL_FILES=(
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/CanonicalRecall.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/memory/SearchMemoryEvidence.kt"
)

ALLOW_CANONICAL_RECALL_UI_FILES=(
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PdfKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/ScreenshotOcrKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PhotoOcrKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/NotePageKeywordSearchViewModel.kt"
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

# --- Allowlist: existing keyword Find surfaces (L1–L4 Retired; MemoryEvidence-backed) --
# Paths relative to repo root. Extend only with ADR + LEGACY_EXTENSION_EXCEPTION.
# L1 SearchPersistedPdfPageText deleted (MIG-07 PDF cutover).
# L2 SearchPersistedScreenshotOcrText deleted (MIG-07 screenshot cutover).
# L3 SearchPersistedPhotoOcrText deleted (MIG-07 photo cutover).
# L4 SearchPersistedNotePageText + NotePageKeywordSearchSupport deleted (MIG-07 note).
ALLOWLIST_KEYWORD_FIND=(
  # PDF keyword UI / helpers (L1 Retired — MemoryEvidence-backed; no SearchPersisted*)
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/LoadPersistedPdfKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/PdfKeywordSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/PdfKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/documents/PdfMemoryEvidenceKeywordAdapter.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PdfKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PdfKeywordSearchCopy.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PdfKeywordSearchHighlight.kt"
  # Screenshot OCR keyword UI / helpers (L2 Retired — MemoryEvidence-backed)
  "MemoraApp/app/src/main/java/com/memora/app/application/images/LoadPersistedScreenshotOcrKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/ScreenshotOcrKeywordSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/ScreenshotOcrKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/ScreenshotMemoryEvidenceKeywordAdapter.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/ScreenshotOcrKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/ScreenshotOcrKeywordSearchScreen.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/ScreenshotOcrKeywordSearchCopy.kt"
  # Photo OCR keyword UI / helpers (L3 Retired — MemoryEvidence-backed)
  "MemoraApp/app/src/main/java/com/memora/app/application/images/LoadPersistedPhotoOcrKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/PhotoOcrKeywordSearchSupport.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/PhotoOcrKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/images/PhotoMemoryEvidenceKeywordAdapter.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PhotoOcrKeywordSearchViewModel.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PhotoOcrKeywordSearchScreen.kt"
  "MemoraApp/app/src/main/java/com/memora/app/ui/search/PhotoOcrKeywordSearchCopy.kt"
  # Note keyword UI / helpers (L4 Retired — MemoryEvidence-backed; no SearchPersisted*)
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/LoadPersistedNotePageKeywordSearchReadiness.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/NotePageKeywordSearchModels.kt"
  "MemoraApp/app/src/main/java/com/memora/app/application/notes/NoteMemoryEvidenceKeywordAdapter.kt"
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
  for a in "${ALLOW_SEARCH_MEMORY_EVIDENCE_UI_FILES[@]}"; do
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
  ok "all KeywordSearch/SearchPersisted files are allowlisted (L1–L4 helpers + PDF highlight)"
fi

# --- C: CanonicalRecall confined to façade + keyword ViewModels ---------------
echo "== C: CanonicalRecall allowlist (thin KEYWORD façade) =="
c_failed=0
c_tmp="$(mktemp)"
grep -RIn --include='*.kt' --include='*.java' -E '\bCanonicalRecall\b' "${MAIN}" \
  >"${c_tmp}" 2>/dev/null || true
while IFS= read -r line || [[ -n "${line}" ]]; do
  [[ -z "${line}" ]] && continue
  file="${line%%:*}"
  rel="${file#./}"
  rel="${rel//\\//}"
  if [[ "${rel}" == *"/ui/"* ]]; then
    local_ok=0
    for a in "${ALLOW_CANONICAL_RECALL_UI_FILES[@]}"; do
      if [[ "${rel}" == "${a}" ]]; then
        local_ok=1
        break
      fi
    done
    if [[ "${local_ok}" -eq 0 ]]; then
      fail "CanonicalRecall under unauthorized ui/ path: ${rel}"
      echo "  ${line}" >&2
      c_failed=1
    fi
    continue
  fi
  app_ok=0
  for a in "${ALLOW_CANONICAL_RECALL_FILES[@]}"; do
    if [[ "${rel}" == "${a}" ]]; then
      app_ok=1
      break
    fi
  done
  if [[ "${app_ok}" -eq 0 ]]; then
    fail "CanonicalRecall outside thin-façade allowlist: ${rel}"
    echo "  ${line}" >&2
    c_failed=1
  fi
done < "${c_tmp}"
rm -f "${c_tmp}"
if [[ "${c_failed}" -eq 0 ]]; then
  ok "CanonicalRecall confined to application façade + keyword ViewModels"
fi

# --- D: SearchMemoryEvidence — candidate gen; forbid UI binding --------------
echo "== D: SearchMemoryEvidence allowlist (no ui/**; CanonicalRecall owns Find) =="
d_failed=0
d_tmp="$(mktemp)"
grep -RIn --include='*.kt' --include='*.java' -E '\bSearchMemoryEvidence\b' "${MAIN}" \
  >"${d_tmp}" 2>/dev/null || true
while IFS= read -r line || [[ -n "${line}" ]]; do
  [[ -z "${line}" ]] && continue
  file="${line%%:*}"
  rel="${file#./}"
  rel="${rel//\\//}"
  # Mentions in PersistenceModule / MemoryDao / Room adapter KDoc are OK.
  if [[ "${rel}" == "MemoraApp/app/src/main/java/com/memora/app/data/di/PersistenceModule.kt" ]] ||
     [[ "${rel}" == "MemoraApp/app/src/main/java/com/memora/app/data/local/MemoryDao.kt" ]] ||
     [[ "${rel}" == "MemoraApp/app/src/main/java/com/memora/app/data/local/RoomMemoryEvidenceExcerptSearch.kt" ]]; then
    continue
  fi
  if [[ "${rel}" == *"/ui/"* ]]; then
    fail "SearchMemoryEvidence under ui/ (use CanonicalRecall): ${rel}"
    echo "  ${line}" >&2
    d_failed=1
    continue
  fi
  if ! is_allowed_search_memory_evidence_file "${rel}"; then
    fail "SearchMemoryEvidence outside MIG-06/07 allowlist: ${rel}"
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

# L1 resurrection: SearchPersistedPdfPageText must stay deleted
if [[ -f "${MAIN_JAVA}/com/memora/app/application/documents/SearchPersistedPdfPageText.kt" ]]; then
  fail "SearchPersistedPdfPageText.kt resurrected (L1 Retired — MIG-07 PDF)"
  d_failed=1
fi

# L2 resurrection: SearchPersistedScreenshotOcrText must stay deleted
if [[ -f "${MAIN_JAVA}/com/memora/app/application/images/SearchPersistedScreenshotOcrText.kt" ]]; then
  fail "SearchPersistedScreenshotOcrText.kt resurrected (L2 Retired — MIG-07 screenshot)"
  d_failed=1
fi

# L3 resurrection: SearchPersistedPhotoOcrText must stay deleted
if [[ -f "${MAIN_JAVA}/com/memora/app/application/images/SearchPersistedPhotoOcrText.kt" ]]; then
  fail "SearchPersistedPhotoOcrText.kt resurrected (L3 Retired — MIG-07 photo)"
  d_failed=1
fi

# L4 resurrection: SearchPersistedNotePageText must stay deleted
if [[ -f "${MAIN_JAVA}/com/memora/app/application/notes/SearchPersistedNotePageText.kt" ]]; then
  fail "SearchPersistedNotePageText.kt resurrected (L4 Retired — MIG-07 note)"
  d_failed=1
fi

if [[ "${d_failed}" -eq 0 ]]; then
  ok "SearchMemoryEvidence confined to candidate-gen allowlist (no ui/**)"
fi

echo
if [[ "${failures}" -gt 0 ]]; then
  echo "legacy-recall-surface: ${failures} failure(s)" >&2
  exit 1
fi
echo "legacy-recall-surface: PASS"
exit 0
