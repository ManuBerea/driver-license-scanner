#!/usr/bin/env python3
import argparse
import json
import mimetypes
import os
import sys
import uuid
from pathlib import Path
from typing import Dict, List, Tuple
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


REQUIRED_FIELDS = [
    "firstName",
    "lastName",
    "dateOfBirth",
    "addressLine",
    "licenceNumber",
    "expiryDate",
]


def build_multipart(field_name: str, file_path: Path) -> Tuple[bytes, str]:
    boundary = f"----dls-{uuid.uuid4().hex}"
    mime, _ = mimetypes.guess_type(str(file_path))
    if not mime:
        mime = "application/octet-stream"

    filename = file_path.name
    with file_path.open("rb") as handle:
        file_bytes = handle.read()

    parts: List[bytes] = []
    parts.append(f"--{boundary}\r\n".encode("utf-8"))
    parts.append(
        f'Content-Disposition: form-data; name="{field_name}"; filename="{filename}"\r\n'.encode("utf-8")
    )
    parts.append(f"Content-Type: {mime}\r\n\r\n".encode("utf-8"))
    parts.append(file_bytes)
    parts.append(b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode("utf-8"))
    body = b"".join(parts)
    return body, boundary


def post_image(url: str, image_path: Path, timeout: int) -> Dict:
    body, boundary = build_multipart("image", image_path)
    req = Request(url, method="POST")
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    req.add_header("Accept", "application/json")
    try:
        with urlopen(req, data=body, timeout=timeout) as resp:
            payload = resp.read()
    except HTTPError as exc:
        payload = exc.read()
    except URLError as exc:
        raise RuntimeError(f"Failed to call API: {exc}") from exc

    try:
        return json.loads(payload.decode("utf-8"))
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"Invalid JSON response: {payload[:200]!r}") from exc


def normalize_text(value: str) -> str:
    if not value:
        return ""
    return " ".join(value.strip().split()).upper()


def normalize_address(value: str) -> str:
    if not value:
        return ""
    normalized = " ".join(value.strip().split())
    normalized = normalized.replace(" ,", ",").replace(", ", ", ")
    return normalized.upper()


def normalize_licence(value: str) -> str:
    if not value:
        return ""
    return "".join(ch for ch in value if ch.isalnum()).upper()


def compare_fields(expected: Dict, actual: Dict, include_categories: bool) -> Tuple[Dict, Dict]:
    mismatches: Dict[str, Dict] = {}
    field_stats: Dict[str, Dict[str, int]] = {}

    fields_to_check = list(REQUIRED_FIELDS)
    if include_categories:
        fields_to_check.append("categories")

    for field in fields_to_check:
        exp = expected.get(field)
        act = actual.get(field)

        if field == "addressLine":
            match = normalize_address(exp) == normalize_address(act)
        elif field == "licenceNumber":
            match = normalize_licence(exp) == normalize_licence(act)
        elif field == "categories":
            exp_list = [str(item).strip().upper() for item in (exp or []) if str(item).strip()]
            act_list = [str(item).strip().upper() for item in (act or []) if str(item).strip()]
            match = sorted(exp_list) == sorted(act_list)
        else:
            match = normalize_text(exp) == normalize_text(act)

        stats = field_stats.setdefault(field, {"total": 0, "correct": 0, "missing": 0, "mismatch": 0})
        stats["total"] += 1

        if match:
            stats["correct"] += 1
            continue

        if not act:
            stats["missing"] += 1
        else:
            stats["mismatch"] += 1

        mismatches[field] = {"expected": exp, "actual": act}

    return mismatches, field_stats


def merge_stats(total: Dict[str, Dict[str, int]], part: Dict[str, Dict[str, int]]) -> None:
    for field, stats in part.items():
        bucket = total.setdefault(field, {"total": 0, "correct": 0, "missing": 0, "mismatch": 0})
        for key, value in stats.items():
            bucket[key] += value


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare /license/scan outputs against ground truth.")
    parser.add_argument("--api-url", default=os.getenv("SCAN_API_URL", "http://localhost:8080/license/scan"))
    parser.add_argument("--dataset", default=os.getenv("EVAL_DATASET_DIR", "docs/images"))
    parser.add_argument("--timeout", type=int, default=30)
    parser.add_argument("--include-categories", action="store_true")
    parser.add_argument("--out", default="reports/scan_output_diff.json")
    args = parser.parse_args()

    dataset_root = Path(args.dataset)
    if not dataset_root.exists():
        repo_root = Path(__file__).resolve().parents[1]
        dataset_root = repo_root / args.dataset
    truth_dir = dataset_root / "ground_truth"
    if not truth_dir.exists():
        print(f"Ground truth folder not found: {truth_dir}", file=sys.stderr)
        return 1

    truth_files = sorted(truth_dir.glob("*.json"))
    if not truth_files:
        print(f"No ground truth files found in {truth_dir}", file=sys.stderr)
        return 1

    results: List[Dict] = []
    stats_total: Dict[str, Dict[str, int]] = {}

    for truth_file in truth_files:
        truth = json.loads(truth_file.read_text(encoding="utf-8"))
        image_rel = truth.get("image")
        if not image_rel:
            continue
        image_path = dataset_root / image_rel
        if not image_path.exists():
            print(f"Missing image: {image_path}", file=sys.stderr)
            continue

        api_response = post_image(args.api_url, image_path, args.timeout)
        expected_fields = truth.get("fields", {})
        actual_fields = api_response.get("fields", {})

        mismatches, field_stats = compare_fields(expected_fields, actual_fields, args.include_categories)
        merge_stats(stats_total, field_stats)

        results.append(
            {
                "id": truth_file.name,
                "image": str(image_path),
                "mismatches": mismatches,
                "expected": expected_fields,
                "actual": actual_fields,
            }
        )

    report_path = Path(args.out)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps({"results": results, "stats": stats_total}, indent=2), encoding="utf-8")

    print(f"Wrote report: {report_path}")
    print("Field accuracy summary:")
    for field, stats in stats_total.items():
        total = stats["total"]
        correct = stats["correct"]
        accuracy = 0 if total == 0 else (correct / total) * 100
        print(f"- {field}: {accuracy:.1f}% ({correct}/{total})")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
