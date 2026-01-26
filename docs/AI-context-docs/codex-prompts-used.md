# Codex Ticket Prompts

## How to use (copy/paste workflow)
Before every ticket, paste these lines above the ticket prompt:

Follow `.github/copilot-instructions.md` strictly.
Consider code quality, adherence to best practices, readability and maintainability.
Only implement this ticket. Do not refactor unrelated code.

---

## Prompt-T3 — Web: capture/upload licence image
```md
## Objective
Implement **Story 01**: allow motor trade staff to **capture** a UK driving license image via **webcam** OR **upload a file**, as the first step of the scan flow.

## User Story Context
User must be able to start the scan process without manual typing by selecting/capturing an image. This screen is the entry point before preview + scan.

## Constraints (must follow)
- Supported formats: **.jpg, .jpeg, .png, .webp**
- Enforce a **max file size limit** (choose **10MB** and use it consistently)
- Invalid inputs must show a **clear user-friendly error** (no crashes, no stack traces)
- Store the selected/captured image **only in memory/state** (no persistence, no uploads yet)
- User can proceed **only after** selecting/capturing an image

## Task
Create a UI component/screen that supports:
1) **File upload**
    - Accept only the supported formats
    - Reject invalid type with readable error
    - Reject >10MB with readable error
2) **Webcam capture**
    - Use `getUserMedia`
    - Allow capture of a frame as an image (Blob/File)
    - Handle permission denied / camera unavailable with a readable error
3) **State + proceed**
    - Store selected image in state
    - Enable a **Continue** button only when an image exists
    - Continue should pass the image to the next step (preview screen will be built in next ticket)

## Deliverables
- React component (and minimal helper utilities if needed)
- Reusable validation helper for type + size (optional, small)

## Verification
- Describe validation rules (type + max size) and how they are enforced
- Confirm the app state contains an image object only after capture/upload
- Confirm Continue is disabled until an image exists

## Stop when
Webcam capture + file upload both work, validation errors render correctly, and the user can proceed only when an image is selected.
```

---

## Prompt-T4 — Web: preview + retry before scan
```md
## Objective
Implement the **preview step** after capture/upload (Story 01 / T3). User must review the selected licence image, retry if needed, and start scanning only when they click **Scan**.

## Context
This screen is shown after an image is selected in T3. It must:
- display the image clearly
- allow replacing it (retry)
- gate the scan request behind an explicit button click

## Constraints (must follow)
- Do NOT upload/scan automatically on navigation to preview
- Retry must work **without page refresh**
- User should never be stuck: always allow going back to choose/retake
- Keep image stored **in memory/state only** (no persistence)
- User-friendly errors only (no stack traces)

## Task
Create a preview screen/component that:
1) **Shows the selected image**
    - display a preview of the captured/uploaded licence image
2) **Retry controls**
    - “Choose another” → returns to file upload flow and replaces image
    - “Retake” → returns to webcam capture flow and replaces image
    - after retry, the preview updates correctly with the new image
3) **Scan control**
    - Provide a **Scan** button
    - Scan button triggers the scan request **only on click**
    - Disable Scan if no image exists (defensive)
    - (Optional) show basic loading state placeholder (actual API wiring is next ticket)

## Deliverables
- Preview screen/component integrated into the scan flow
- Retry actions that correctly replace the image in state

## Verification
- Confirm preview loads without calling the scan API automatically
- Confirm pressing Scan is the only trigger for the scan request
- Confirm retry replaces the image correctly and preview updates

## Stop when
Preview shows the image, retry replaces it without refresh, and Scan is gated on an explicit user click.
```

---

## Prompt-T5 — API: `/license/scan` endpoint stub
```md
## Objective
Create stub scan endpoint with correct response + error shape.

## Constraints
- Do NOT log image bytes
- Do NOT log OCR text or extracted fields
- Must include header: `Cache-Control: no-store`
- Validate formats: jpg/jpeg/png/webp

## Task
Implement `POST /license/scan`:
- Accept multipart field `image`
- Validate presence + format
- Return contract with empty/null `fields` (stub)
- Standard error codes: INVALID_IMAGE, OCR_FAILED, OCR_TIMEOUT, PARSING_FAILED

## Deliverables
- Controller + DTOs
- Error response shape
- Cache-Control header set

## Verification
- Provide curl request + sample responses

## Stop when
Endpoint returns correct stub JSON and invalid upload returns INVALID_IMAGE.
```

---

## Prompt-T6 — OCR worker: `/ocr` stub
```md
## Objective
Implement worker endpoints: GET /health and POST /ocr (stub response).

## Constraints
- Do NOT persist uploaded files to disk
- Do NOT log OCR text or file contents

## Task
Implement:
- POST /ocr accepts multipart `image` and returns:
  { requestId, engine:"stub", confidence:0.0, lines:[], processingTimeMs:1 }

## Deliverables
- FastAPI app routing + minimal models

## Verification
- curl /ocr
- confirm no files written to disk

## Stop when
Endpoints run and return correct JSON shapes.
```

---

## Prompt-T7 — Web: call scan API + loading/error/retry
```md
## Objective
Wire Scan button to API with loading + error + retry UX.

## Constraints
- No stack traces to user
- Retry without refresh

## Task
Implement:
- API client: multipart POST to `/license/scan`
- loading state
- error UI + retry
- store success response for next screen (form)

## Deliverables
- API client module
- UI wiring

## Verification
- Describe state transitions: idle → loading → success/error → retry

## Stop when
Scan request works with correct UX behavior.
```

---

## Prompt-T8 — OCR Worker: PaddleOCR integration
```md
## Objective
Replace stub OCR with PaddleOCR and return real lines + confidence.

## Constraints
- No persistence
- No PII logs
- Response includes: requestId, engine, confidence 0..1, lines[], processingTimeMs

## Task
Implement PaddleOCR:
- extract text lines + per-line confidence
- compute overall confidence (0..1)
- measure processing time

## Deliverables
- PaddleOCR engine implementation
- /ocr uses PaddleOCR

## Verification
- Run /ocr with synthetic sample; response contains non-empty lines

## Stop when
/ocr returns real lines and confidence for clean synthetic input.
```

---

## Prompt-T9 — Worker: engine selection + confidence (minimal)
```md
## Objective
Add OCR engine selection (paddle|doctr|vision|textract) with consistent OcrResult output.

## Constraints
- Default engine: paddle
- vision/textract opt-in only; without config -> fail gracefully
- No PII logs
- Keep changes minimal (no major refactors)

## Task
1) Support `OCR_ENGINE` env: paddle|doctr|vision|textract
2) Add docTR local engine support
3) Add cloud engine placeholders behind flags (return OCR_FAILED if selected without config)
4) Compute overall confidence deterministically (avg per-line confidence; fallback 0.5 if unavailable)

## Deliverables
- Engine selection works + shared output shape

## Verification
- OCR_ENGINE=doctr works locally
- OCR_ENGINE=vision fails gracefully without creds/config

## Stop when
Engine selection works and all engines return same output schema.
```

---

## Prompt-T10 — API: `OcrClient` calls worker + timeout + error mapping
```md
## Objective
API calls OCR worker safely with timeout + internal key and maps failures to standard codes.

## Constraints
- No PII logs
- Timeout -> OCR_TIMEOUT
- Other worker failure -> OCR_FAILED
- Keep changes minimal; do not change `/license/scan` response schema

## Task
Implement API OCR client:
- base URL from `OCR_WORKER_URL`
- header `X-INTERNAL-KEY` from `INTERNAL_KEY`
- timeout default ~8s
- safe error mapping

## Deliverables
- OcrClient class/service
- error mapping implemented

## Verification
- success path described
- worker down/timeout returns OCR_TIMEOUT safely

## Stop when
API can call worker and returns standardized error codes on failures.
```

---

## Prompt-T11 — API: deterministic parser to required fields
```md
## Objective
Parse OCR lines into structured fields deterministically.

## Constraints
- No guessing (unknowns null/empty)
- Normalize dates to YYYY-MM-DD
- Must not crash on malformed OCR

## Task
Extract:
- firstName, lastName, dateOfBirth
- addressLine, postcode
- licenceNumber, expiryDate
Optional: categories

## Deliverables
- Parser implementation (unit test optional here)

## Verification
- Show synthetic OCR input lines + expected parsed output

## Stop when
Parser returns best-effort fields and leaves unknowns empty.
```

---

## Prompt-T12 — API: full scan contract (OCR + parse + validation stub + no-store)
```md
## Objective
Return the final scan response contract to frontend.

## Constraints
- Must include `Cache-Control: no-store`
- No raw OCR in response unless dev/debug flag
- No PII logs

## Task
Update `/license/scan` to:
- call OCR worker
- parse fields
- return:
  requestId, selectedEngine, attemptedEngines, ocrConfidence, processingTimeMs, fields, validation

(validation can be empty arrays until T15.)

## Deliverables
- Working scan endpoint returning contract

## Verification
- Curl request returns JSON with expected fields + header set

## Stop when
Scan endpoint returns correct contract reliably.
```

---

## Prompt-T13 — Web: editable driver form + low-confidence warning
```md
## Objective
Show editable form auto-filled from scan response + low-confidence warning.

## Constraints
- Warning text EXACT:
  “Low confidence — please review fields”
- Threshold configurable (do not hardcode 0.70)
- All fields editable

## Task
Render editable fields:
- firstName, lastName, dateOfBirth, addressLine, postcode, licenceNumber, expiryDate
Populate from scan response.
Show warning banner if confidence below threshold.

## Deliverables
- Form UI + warning banner

## Verification
- Explain how threshold is configured and used

## Stop when
Form renders, autofills, edits work, warning works.
```

---

## Prompt-T14 — API: backend validation rules (blocking + warnings)
```md
## Objective
Validate extracted fields and return structured validation results.

## Constraints
- blockingErrors[] with field/code/message
- warnings[]
- No PII logs

## Task
Implement:
Blocking:
- expiry in past
- missing required fields
- invalid UK postcode
- invalid UK licence number
Warning:
- age outside 21–75

## Deliverables
- Validator + ValidationResult model

## Verification
- Provide synthetic examples for blocking + warning outputs

## Stop when
Validation returns correct results for common invalid cases.
```

---

## Prompt-T15 — Web: field-level errors + block submit
```md
## Objective
Display API validation errors and block submit when invalid.

## Constraints
- User can still edit fields when errors exist

## Task
- Map `blockingErrors` to field inputs
- Show warnings in banner/list
- Disable submit/save while blockingErrors exist

## Deliverables
- Updated form behavior

## Verification
- Show error-to-field mapping logic and disabled submit condition

## Stop when
Errors render correctly and submit is blocked when invalid.
```

---

## Prompt-T16 — API: fallback OCR orchestration (optional, flagged)
```md
## Objective
Add optional fallback OCR strategy for low quality results.

## Constraints
- OFF by default (`ENABLE_FALLBACK_OCR=false`)
- Keep latency bounded (`MAX_FALLBACK_ATTEMPTS=2`)
- attemptedEngines + selectedEngine must reflect behavior
- No PII logs

## Task
Fallback triggers if:
- ocrConfidence < threshold OR
- required fields missing after parsing OR
- OCR fails/timeouts

Order: paddle → doctr → vision → textract

## Deliverables
- fallback orchestration
- response reflects attempted engines

## Verification
- Demonstrate behavior with fallback disabled vs enabled

## Stop when
Fallback works safely and metadata reflects attempts.
```

---

## Prompt-T17 — Synthetic dataset structure + ground truth templates
```md
## Objective
Create dataset folder structure for evaluation (synthetic only).

## Constraints
- Synthetic/anonymized only
- 10 clean / 10 medium / 10 poor
- Ground truth JSON per image

## Task
Create:
- `data/synthetic/clean`
- `data/synthetic/medium`
- `data/synthetic/poor`
- `data/synthetic/ground_truth`

Add ground truth JSON templates (placeholders ok if images not ready).

## Deliverables
- Folder structure + example ground truth schema

## Verification
- Confirm schema completeness for required fields

## Stop when
Dataset structure exists and ground truth schema is ready for 30 items.
```

---

## Prompt-T18 — Evaluation harness + OCR comparison report (minimal)
```md
## Objective
Generate `reports/ocr_engine_comparison.md` with accuracy + latency metrics.

## Constraints
- Repeatable locally
- No PII logs (do not print OCR text or field values)
- Prefer single script + minimal deps
- Output ONE markdown report file

## Task
Create a runner that:
- iterates dataset buckets (clean/medium/poor)
- runs scans via API (preferred)
- computes:
  per-field accuracy, overall required-field accuracy,
  median + p95 scan→form time, % confidence < threshold
- writes markdown tables + pass/fail vs thresholds

## Deliverables
- `tools/eval_runner.py` (or equivalent)
- report output path documented

## Verification
- Provide exact command to run and what it generates

## Stop when
Runner produces a markdown report with tables and pass/fail checks.
```

---

## Prompt-T19 — Staging docs + security/privacy checklist
```md
## Objective
Prepare staging deployment notes and verify privacy/security requirements.

## Constraints
- HTTPS only
- No secrets committed
- No PII logs or persistence

## Task
1) Create `docs/security-checklist.md` with checks:
- no image persistence
- no OCR text logs
- no parsed field logs
- Cache-Control: no-store
- HTTPS staging
- OCR worker protected (internal key/private)

2) Add deployment notes for:
- Web (Vercel)
- API (Render Docker)
- Worker (Render Docker)

## Deliverables
- Completed checklist (not blank)
- Deployment notes (no secrets)

## Verification
- Checklist is filled in
- Notes mention HTTPS-only + internal key

## Stop when
Checklist exists and staging requirements are documented.
```
