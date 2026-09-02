#!/usr/bin/env python3
"""
Validate UNFYND Core Class A synthetic JSON examples.

Implements SPEC-aligned rules for illustrative sketches:
- Truth before intelligence (non-empty evidence for valid Asset Memories)
- Anchors must cite existing evidence IDs (no empty citation lists)
- RETRIEVAL_SIGNAL alone cannot justify durable claims in valid sketches
- Export bundles reference cited evidence slices only

Usage:
  python validate_class_a_examples.py
  python validate_class_a_examples.py --file ../examples/valid-memory-evidence.json
  python validate_class_a_examples.py --self-test
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

EVIDENCE_CLASSES = frozenset(
    {
        "DIRECT",
        "VALIDATED_OBSERVATION",
        "RETRIEVAL_SIGNAL",
        "HYPOTHESIS",
    }
)

TRUTH_JUSTIFYING_CLASSES = frozenset({"DIRECT", "VALIDATED_OBSERVATION"})

SKETCH_KINDS = frozenset(
    {
        "valid_asset_memory",
        "invalid_asset_memory",
        "valid_evidence_package_export",
    }
)


def _err(path: str, message: str) -> str:
    return f"{path}: {message}"


def _require_dict(value: Any, path: str, errors: list[str]) -> dict[str, Any] | None:
    if not isinstance(value, dict):
        errors.append(_err(path, "expected object"))
        return None
    return value


def _require_str(value: Any, path: str, errors: list[str]) -> str | None:
    if not isinstance(value, str) or not value.strip():
        errors.append(_err(path, "expected non-empty string"))
        return None
    return value


def _require_int(value: Any, path: str, errors: list[str], minimum: int = 1) -> int | None:
    if not isinstance(value, int) or value < minimum:
        errors.append(_err(path, f"expected integer >= {minimum}"))
        return None
    return value


def _require_list(value: Any, path: str, errors: list[str]) -> list[Any] | None:
    if not isinstance(value, list):
        errors.append(_err(path, "expected array"))
        return None
    return value


def validate_evidence(evidence: Any, path: str, errors: list[str]) -> dict[str, Any] | None:
    obj = _require_dict(evidence, path, errors)
    if obj is None:
        return None

    eid = _require_str(obj.get("evidenceId"), f"{path}.evidenceId", errors)
    eclass = _require_str(obj.get("evidenceClass"), f"{path}.evidenceClass", errors)
    if eclass is not None and eclass not in EVIDENCE_CLASSES:
        errors.append(_err(f"{path}.evidenceClass", f"unknown class {eclass!r}"))

    _require_str(obj.get("kind"), f"{path}.kind", errors)
    if "excerpt" not in obj or not isinstance(obj.get("excerpt"), str):
        errors.append(_err(f"{path}.excerpt", "expected string excerpt"))
    _require_str(obj.get("provenance"), f"{path}.provenance", errors)

    locator = obj.get("locator")
    if locator is not None:
        loc = _require_dict(locator, f"{path}.locator", errors)
        if loc is not None:
            ltype = _require_str(loc.get("type"), f"{path}.locator.type", errors)
            if ltype == "timecode":
                _require_int(loc.get("startMs"), f"{path}.locator.startMs", errors, minimum=0)
                end = loc.get("endMs")
                if end is not None and (not isinstance(end, int) or end < 0):
                    errors.append(_err(f"{path}.locator.endMs", "expected integer >= 0"))

    return obj


def validate_anchor(anchor: Any, path: str, errors: list[str]) -> dict[str, Any] | None:
    obj = _require_dict(anchor, path, errors)
    if obj is None:
        return None
    _require_str(obj.get("type"), f"{path}.type", errors)
    if "value" not in obj or not isinstance(obj.get("value"), str):
        errors.append(_err(f"{path}.value", "expected string value"))
    cited = _require_list(obj.get("citedEvidenceIds"), f"{path}.citedEvidenceIds", errors)
    if cited is not None:
        for i, item in enumerate(cited):
            if not isinstance(item, str) or not item.strip():
                errors.append(_err(f"{path}.citedEvidenceIds[{i}]", "expected non-empty string id"))
    return obj


def validate_memory_sketch(memory: Any, path: str, errors: list[str]) -> dict[str, Any] | None:
    obj = _require_dict(memory, path, errors)
    if obj is None:
        return None

    _require_str(obj.get("memoryId"), f"{path}.memoryId", errors)
    _require_str(obj.get("assetRef"), f"{path}.assetRef", errors)
    _require_int(obj.get("revisionHint"), f"{path}.revisionHint", errors)
    if "summary" not in obj or not isinstance(obj.get("summary"), str):
        errors.append(_err(f"{path}.summary", "expected string summary"))

    anchors = _require_list(obj.get("anchors"), f"{path}.anchors", errors) or []
    for i, anchor in enumerate(anchors):
        validate_anchor(anchor, f"{path}.anchors[{i}]", errors)

    evidence_list = _require_list(obj.get("evidence"), f"{path}.evidence", errors) or []
    for i, ev in enumerate(evidence_list):
        validate_evidence(ev, f"{path}.evidence[{i}]", errors)

    return obj


def validate_export_bundle(bundle: Any, path: str, errors: list[str]) -> dict[str, Any] | None:
    obj = _require_dict(bundle, path, errors)
    if obj is None:
        return None

    _require_str(obj.get("bundleId"), f"{path}.bundleId", errors)
    _require_str(obj.get("exportedAt"), f"{path}.exportedAt", errors)
    _require_str(obj.get("coreContractRevision"), f"{path}.coreContractRevision", errors)

    memories = _require_list(obj.get("memories"), f"{path}.memories", errors) or []
    if not memories:
        errors.append(_err(f"{path}.memories", "expected at least one memory reference"))

    slice_ids: set[str] = set()
    slices = _require_list(obj.get("evidenceSlices"), f"{path}.evidenceSlices", errors) or []
    if not slices:
        errors.append(_err(f"{path}.evidenceSlices", "expected at least one evidence slice"))

    for i, sl in enumerate(slices):
        ev = validate_evidence(sl, f"{path}.evidenceSlices[{i}]", errors)
        if ev and isinstance(ev.get("evidenceId"), str):
            slice_ids.add(ev["evidenceId"])

    for i, mem in enumerate(memories):
        m = _require_dict(mem, f"{path}.memories[{i}]", errors)
        if m is None:
            continue
        _require_str(m.get("memoryId"), f"{path}.memories[{i}].memoryId", errors)
        _require_int(m.get("revisionHint"), f"{path}.memories[{i}].revisionHint", errors)
        refs = _require_list(m.get("evidenceSliceIds"), f"{path}.memories[{i}].evidenceSliceIds", errors)
        if refs is not None:
            if not refs:
                errors.append(
                    _err(f"{path}.memories[{i}].evidenceSliceIds", "expected at least one slice id")
                )
            for j, ref in enumerate(refs):
                if not isinstance(ref, str) or not ref.strip():
                    errors.append(
                        _err(f"{path}.memories[{i}].evidenceSliceIds[{j}]", "expected non-empty id")
                    )
                elif slice_ids and ref not in slice_ids:
                    errors.append(
                        _err(
                            f"{path}.memories[{i}].evidenceSliceIds[{j}]",
                            f"unknown evidence slice id {ref!r}",
                        )
                    )

    return obj


def validate_document(data: Any, *, expect_valid: bool | None = None) -> list[str]:
    errors: list[str] = []
    root = _require_dict(data, "$", errors)
    if root is None:
        return errors

    sketch_kind = _require_str(root.get("sketchKind"), "$.sketchKind", errors)
    if sketch_kind is not None and sketch_kind not in SKETCH_KINDS:
        errors.append(_err("$.sketchKind", f"unknown sketchKind {sketch_kind!r}"))

    if sketch_kind == "valid_asset_memory":
        memory = validate_memory_sketch(root.get("memory"), "$.memory", errors)
        _require_str(root.get("whyAcceptable"), "$.whyAcceptable", errors)
        if expect_valid is not False and memory is not None:
            apply_valid_asset_memory_rules(memory, errors)
    elif sketch_kind == "invalid_asset_memory":
        validate_memory_sketch(root.get("memory"), "$.memory", errors)
        rejection = _require_dict(root.get("rejection"), "$.rejection", errors)
        if rejection is not None:
            status = _require_str(rejection.get("status"), "$.rejection.status", errors)
            if status is not None and status != "reject":
                errors.append(_err("$.rejection.status", "expected 'reject'"))
            _require_str(rejection.get("why"), "$.rejection.why", errors)
        if expect_valid is True:
            errors.append(_err("$", "document is marked invalid but expected valid"))
    elif sketch_kind == "valid_evidence_package_export":
        validate_export_bundle(root.get("exportBundle"), "$.exportBundle", errors)
        _require_str(root.get("notes"), "$.notes", errors)
    elif sketch_kind is not None:
        errors.append(_err("$.sketchKind", "unsupported sketch kind for validator"))

    return errors


def apply_valid_asset_memory_rules(memory: dict[str, Any], errors: list[str]) -> None:
    evidence_list = memory.get("evidence")
    if not isinstance(evidence_list, list) or len(evidence_list) == 0:
        errors.append(_err("$.memory.evidence", "valid Asset Memory requires non-empty evidence"))
        return

    evidence_by_id: dict[str, dict[str, Any]] = {}
    for i, ev in enumerate(evidence_list):
        if not isinstance(ev, dict):
            continue
        eid = ev.get("evidenceId")
        if isinstance(eid, str) and eid:
            evidence_by_id[eid] = ev

    anchors = memory.get("anchors")
    if isinstance(anchors, list):
        for i, anchor in enumerate(anchors):
            if not isinstance(anchor, dict):
                continue
            cited = anchor.get("citedEvidenceIds")
            if not isinstance(cited, list) or len(cited) == 0:
                errors.append(
                    _err(
                        f"$.memory.anchors[{i}].citedEvidenceIds",
                        "anchors on valid Asset Memory must cite at least one evidence id",
                    )
                )
                continue
            for j, ref in enumerate(cited):
                if not isinstance(ref, str) or ref not in evidence_by_id:
                    errors.append(
                        _err(
                            f"$.memory.anchors[{i}].citedEvidenceIds[{j}]",
                            f"unknown evidence id {ref!r}",
                        )
                    )
                    continue
                ev_class = evidence_by_id[ref].get("evidenceClass")
                if ev_class not in TRUTH_JUSTIFYING_CLASSES:
                    errors.append(
                        _err(
                            f"$.memory.anchors[{i}].citedEvidenceIds[{j}]",
                            f"evidence class {ev_class!r} cannot alone justify a durable anchor claim",
                        )
                    )

    summary = memory.get("summary", "")
    if isinstance(summary, str) and summary.strip():
        has_direct = any(
            isinstance(ev, dict) and ev.get("evidenceClass") in TRUTH_JUSTIFYING_CLASSES
            for ev in evidence_list
        )
        if not has_direct:
            errors.append(
                _err(
                    "$.memory.summary",
                    "valid Asset Memory summary requires at least one DIRECT or VALIDATED_OBSERVATION evidence slice",
                )
            )


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def classify_example(path: Path) -> bool | None:
    name = path.name.lower()
    if name.startswith("valid-"):
        return True
    if name.startswith("invalid-"):
        return False
    return None


def validate_file(path: Path) -> list[str]:
    try:
        data = load_json(path)
    except json.JSONDecodeError as exc:
        return [f"{path}: invalid JSON: {exc}"]

    expect_valid = classify_example(path)
    if expect_valid is False:
        structural = validate_document(data, expect_valid=None)
        if structural:
            return structural
        memory = data.get("memory") if isinstance(data, dict) else None
        if isinstance(memory, dict):
            truth_errors: list[str] = []
            apply_valid_asset_memory_rules(memory, truth_errors)
            if truth_errors:
                return []
        return [f"{path}: invalid example must violate valid Asset Memory rules"]
    return validate_document(data, expect_valid=expect_valid)


def default_examples_dir() -> Path:
    return Path(__file__).resolve().parent.parent / "examples"


def run_self_test() -> int:
    examples_dir = default_examples_dir()
    failures: list[str] = []

    for path in sorted(examples_dir.glob("*.json")):
        errors = validate_file(path)
        expect_valid = classify_example(path)
        if expect_valid is True and errors:
            failures.append(f"{path.name}: expected valid but got: {'; '.join(errors)}")
        elif expect_valid is False and errors:
            failures.append(
                f"{path.name}: expected invalid (truth violation) but got: {'; '.join(errors)}"
            )
        elif expect_valid is None and errors:
            failures.append(f"{path.name}: unclassified example failed: {'; '.join(errors)}")

    if failures:
        for line in failures:
            print(line, file=sys.stderr)
        return 1

    print(f"self-test passed ({len(list(examples_dir.glob('*.json')))} examples)")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate Class A synthetic JSON examples")
    parser.add_argument(
        "--file",
        type=Path,
        help="Validate a single JSON file",
    )
    parser.add_argument(
        "--examples-dir",
        type=Path,
        default=None,
        help="Directory of example JSON files (default: ../examples)",
    )
    parser.add_argument(
        "--self-test",
        action="store_true",
        help="Run built-in example suite expectations",
    )
    args = parser.parse_args(argv)

    if args.self_test:
        return run_self_test()

    paths: list[Path]
    if args.file is not None:
        paths = [args.file]
    else:
        examples_dir = args.examples_dir or default_examples_dir()
        paths = sorted(examples_dir.glob("*.json"))
        if not paths:
            print(f"No JSON files in {examples_dir}", file=sys.stderr)
            return 1

    exit_code = 0
    for path in paths:
        errors = validate_file(path)
        if errors:
            exit_code = 1
            print(f"FAIL {path}")
            for err in errors:
                print(f"  {err}")
        else:
            print(f"OK   {path}")

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
