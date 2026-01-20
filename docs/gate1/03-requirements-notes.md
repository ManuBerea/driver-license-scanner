# Gate 1 — Requirements Notes & Testable Behaviors

This document captures key **cross-cutting requirements** and clarifies how they should be interpreted for testing.

---

## API response contract (minimum)
The API response to the frontend must include at minimum:
- `requestId`: unique identifier for traceability
- `ocrConfidence`: float between 0 and 1
- `fields`: structured driver fields
- `validation`: `{ blockingErrors: [], warnings: [] }`
- Standardized error payloads when failing

**Standard error codes (minimum)**
- `OCR_FAILED` — OCR worker or engine failed unexpectedly
- `OCR_TIMEOUT` — request exceeded configured timeout
- `INVALID_IMAGE` — unsupported format or unreadable file
- `PARSING_FAILED` — OCR succeeded but parsing produced no usable fields

---

## Confidence threshold rules
- Confidence is interpreted as an **overall OCR confidence** for the scan.
- If `ocrConfidence < 0.70`, UI must show the warning banner.
- Fallback orchestration (story 07) may also trigger when confidence is below threshold OR parsing fails OR fields missing.

---

## Deterministic parsing first (safe-by-default)
The default parsing strategy must:
- Prefer deterministic parsing (regex/heuristics)
- Return **null/empty string** when uncertain
- Never guess values to “look complete”
- Never throw unhandled exceptions

Optional LLM cleanup can exist **behind a feature flag** and must be documented.

---

## Privacy and logging rules (non-negotiable)
- Never persist raw images by default
- Never log OCR text or extracted field values
- Never include PII in error payloads
- Use `Cache-Control: no-store` on API responses
- Protect internal OCR worker calls with an internal header (e.g., `X-INTERNAL-KEY`)

---

## Dataset-driven evaluation intent
The POC is evaluated against a synthetic dataset with quality buckets:
- 10 clean
- 10 medium
- 10 poor

Required metrics:
- ≥ 85% required-field accuracy
- Median capture → populated form ≤ 10 seconds

Stretch reporting:
- field-by-field accuracy
- engine comparisons
- failure pattern analysis
