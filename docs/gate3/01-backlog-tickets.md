# Gate 3 - Backlog (20 AI-Agent-Ready Tickets)

# v0 - Capture + Preview + Raw OCR Output

## T1 - Monorepo baseline + docs scaffold

### Goal
Create a clean monorepo foundation (folders + ignore rules + docs index).

### Context
We are building 3 services in one repo and must keep gates documentation under `docs/`.

### Task
- Ensure root repo contains:
  - `.gitignore` ignoring `.env`, `.venv`, `node_modules`, build outputs
  - `docs/README.md` index linking gates
  - folders: `web/`, `driver-license-scanner-api/`, `ocr-worker/`

### Requirements
- Do not commit secrets (`.env`) or generated folders (`.venv`, `node_modules`)
- Do not create nested git repos inside subfolders

### Acceptance Criteria
- No nested git repos inside subfolders (no `web/.git/`)
- Repo structure matches Global constraints above
- `git status` shows only source files (not virtualenv / node_modules)

---

## T2 - Docker Compose: run all services locally (dev parity)

### Goal
Enable 1-command local boot for Web + API + OCR worker.

### Context
We need 1-command local run for fast iteration.

### Task
Create root `docker-compose.yml` with 3 services:
- `web` - port 3000
- `api` - port 8080
- `ocr-worker` - port 8000

### Requirements
- `docker compose up --build` starts all 3 services
- API exposes a health route
- OCR Worker exposes `/health` and `/docs`
- No PII printed in logs

### Acceptance Criteria
- `docker compose up --build` starts all services
- Each service is reachable:
  - Web: http://localhost:3000
  - API: http://localhost:8080 (health route exists)
  - OCR Worker: http://localhost:8000/health and /docs
- No PII printed in Docker logs

---

## T3 - Web: capture/upload licence image

### Goal
User can capture via webcam OR upload a licence image.

### Context
This is the first step of the scan flow.

### Task
Implement UI that supports:
- Webcam capture (`getUserMedia`) OR file upload
- Validation of file type and size
- Store selected image in memory/state (not persisted)

### Requirements
- Allowed formats: `.jpg .jpeg .png .webp`
- Max file size: 10MB
- Clear, user-friendly error message on invalid inputs

### Acceptance Criteria
- User can successfully select/capture an image
- Unsupported format is rejected with a readable error
- Oversize files are rejected with a readable error

---

## T4 - Web: preview + retry before scan

### Goal
User previews the image and can retry before sending to backend.

### Context
Preview + retry improves OCR accuracy and matches the required user flow.

### Task
Add a preview screen with:
- Image preview
- Retry controls: Choose another / Retake
- Scan button that triggers the backend call

### Requirements
- Do not upload automatically; only upload when user clicks Scan
- Retry works without refreshing the page

### Acceptance Criteria
- Preview is visible
- Retry replaces the selected image
- Scan triggers a network request only on click

---

## T5 - API: `/license/scan` endpoint (stub response)

### Goal
Accept image upload and return a valid response contract (stubbed at first).

### Context
The web app calls the API (not the OCR worker directly).

### Task
Create `POST /license/scan` that:
- Accepts multipart file field `image`
- Validates input (presence + format)
- Returns a response matching the contract (fields can be empty/null for now)
- Sets `Cache-Control: no-store`

### Requirements
- Never log image bytes
- Return standardized errors (`INVALID_IMAGE` etc.)

### Acceptance Criteria
- Valid request returns 200 with contract JSON
- Invalid file returns error JSON with `INVALID_IMAGE`
- Response includes `Cache-Control: no-store`

---

## T6 - OCR Worker: `/health` + `/ocr` endpoint (stub)

### Goal
Provide an OCR service surface area so the API can integrate.

### Context
OCR runs server-side; this worker should never persist images.

### Task
Implement:
- `GET /health` - `200 { "status": "ok" }`
- `POST /ocr` accepts multipart `image` and returns stub:
```json
{
  "requestId":"uuid",
  "engine":"stub",
  "confidence":0.0,
  "lines":[],
  "processingTimeMs":1
}
```

### Requirements
- No file persistence and no PII logs

### Acceptance Criteria
- `/health` returns 200
- `/ocr` returns correct JSON shape
- No disk writes of uploaded images

---

## T7 - Web: call scan API + loading/error/retry UX

### Goal
User can press Scan, wait, see results, or retry on failure.

### Context
Errors must be user-friendly and the user must be able to retry without refresh.

### Task
Wire the Scan button to:
- POST multipart to `/license/scan`
- Show loading state until response
- On error, show message + allow retry

### Requirements
- No stack traces in UI
- Retry must re-attempt the request

### Acceptance Criteria
- Loading indicator shows during request
- Failures show a readable message and a retry path
- Retry works without page refresh

---

# v0.1 - Real OCR + Parsing + Auto-fill Form

## T8 - OCR Worker: PaddleOCR integration

### Goal
Return real OCR `lines[]`, confidence, and timing.

### Context
PaddleOCR is the primary engine for the POC.

### Task
Integrate PaddleOCR in `/ocr`:
- Extract `lines[]` with `text` + `confidence`
- Return overall `confidence` (0..1)
- Return `processingTimeMs`

### Requirements
- No persistence and no PII logs
- Include bounding boxes per line if available (optional for POC)

### Acceptance Criteria
- Clean synthetic input produces non-empty `lines[]`
- Overall confidence is within 0..1
- Response contains timing

---

## T9 - OCR engine selection abstraction (+ docTR + cloud placeholders)

### Goal
Support selecting OCR engines via config to enable later benchmarking.

### Context
POC must compare multiple OCR engines; cloud engines must be opt-in.

### Task
Implement OCR engine selection with env/config:
- `OCR_ENGINE=paddle|vision` (default `paddle`)
- Add placeholders for Vision behind explicit enable flags

### Requirements
- Vision must be **disabled by default**
- If a cloud engine is selected without credentials, return `OCR_FAILED` gracefully
- Common `OcrResult` response shape for all engines
- No PII logs

### Acceptance Criteria
- Switching `OCR_ENGINE` changes engine behavior (paddle/vision)
- Selecting cloud engine without enable+keys fails gracefully (no crash)

---

## T10 - API: OCR client calls worker (internal)

### Goal
API calls OCR worker reliably with safe failure handling.

### Context
The web app must only talk to the API. API talks to OCR worker.

### Task
Implement API - OCR worker call:
- Use env var `OCR_WORKER_URL`
- Add internal auth header `X-INTERNAL-KEY`
- Implement timeout + error mapping:
  - timeout - `OCR_TIMEOUT`
  - failure - `OCR_FAILED`

### Requirements
- Never log OCR text or parsed fields
- Do not persist uploads

### Acceptance Criteria
- API can successfully call worker and receive `OcrResult`
- Failures return standardized error payloads

---

## T11 - API: deterministic parser to required fields

### Goal
Extract structured driver fields from OCR output without guessing.

### Context
POC requires deterministic, testable parsing (no hallucinations).

### Task
Parse OCR `lines[]` into:
- firstName, lastName, dateOfBirth
- addressLine, postcode
- licenceNumber, expiryDate
Optional: categories

### Requirements
- Normalize whitespace
- Normalize dates to ISO format `YYYY-MM-DD`
- If uncertain, return null/empty (no guessing)
- No unhandled exceptions

### Acceptance Criteria
- For clean/medium OCR results, fields are extracted when present
- Unknown fields remain null/empty
- Parser never crashes the request

---

## T12 - API: scan response contract (fields + validation + headers)

### Goal
Return stable scan response to the web app.

### Context
The web form depends on a stable schema and safe headers.

### Task
Update `/license/scan` to:
- Call OCR worker
- Parse fields
- Return full contract:
  - requestId, selectedEngine, attemptedEngines, ocrConfidence, processingTimeMs
  - fields
  - validation (empty arrays for now)
- Set `Cache-Control: no-store`

### Requirements
- Do not include raw OCR text unless v0 debug mode is enabled
- No PII logs

### Acceptance Criteria
- Response matches the contract exactly
- Headers include `Cache-Control: no-store`

---

## T13 - Web: editable driver form + low-confidence warning

### Goal
Auto-fill an editable form and warn users on low confidence.

### Context
User must be able to correct OCR mistakes and review low-confidence scans.

### Task
- Render an editable form with required fields
- Populate fields from API response
- Show warning banner if `ocrConfidence < threshold`

### Requirements
- The warning threshold must be configurable (do not hardcode 0.70 in UI)
  - Use a value provided by API OR a config value sent by backend
- Show warning text exactly:
  **Low confidence - please review fields**
- Highlight missing required fields

### Acceptance Criteria
- Form is populated without refresh
- User can edit all fields
- Warning appears when confidence is below threshold

---

# v1 - Validation + Fallback + Quality + Benchmarking + Deployment

## T14 - API: backend validation rules (blocking + warnings)

### Goal
Validate extracted/user-edited fields and return structured validation results.

### Context
Validation must prevent invalid submissions and show errors per field.

### Task
Implement validation rules:
- Expiry date in past - blocking error
- Missing required fields - blocking errors
- Invalid UK postcode - blocking error
- Invalid licence number - blocking error
- Age outside 21-75 - warning

### Requirements
Return `validation`:
- `blockingErrors[]`: `{ field, code, message }`
- `warnings[]`: `{ code, message }` (or strings)

### Acceptance Criteria
- Validation results are included in scan response
- Blocking errors are field-specific and readable

---

## T15 - Web: field-level errors + block submit when invalid

### Goal
Display validation errors and prevent submission when blocking errors exist.

### Context
Users must fix issues before completing the flow.

### Task
- Display `blockingErrors` next to the relevant input fields
- Disable Submit/Save if blocking errors exist
- Display non-blocking warnings in a banner/list

### Requirements
- UI remains editable even when errors are present
- No crash on missing/partial validation payloads

### Acceptance Criteria
- Field errors appear next to correct inputs
- Submit is disabled while blocking errors exist

---

## T16 - API: fallback OCR orchestration (optional, flagged)

### Goal
Optionally retry with a different OCR engine when results are poor.

### Context
Fallback is required for improved success rate but must be off by default.

### Task
Implement optional fallback logic:
- Flag: `ENABLE_FALLBACK_OCR=false` by default
- Trigger fallback when:
  - `ocrConfidence < 0.70`, OR
  - required fields missing after parsing, OR
  - OCR fails/timeouts
- Fallback order (recommended): paddle -> vision

### Requirements
- Response must include:
  - `attemptedEngines[]` (in order)
  - `selectedEngine` (final)
- Keep total attempts/time bounded (avoid extreme latency)
- No PII logs

### Acceptance Criteria
- Fallback only runs when enabled
- `attemptedEngines` accurately lists all attempts
- `selectedEngine` matches final engine used

---

## T17 - Synthetic dataset (30 images) + ground truth

### Goal
Create a safe dataset for evaluation: 10 clean / 10 medium / 10 poor.

### Context
Benchmarking requires a repeatable synthetic dataset (no real PII).

### Task
Create:
- `data/synthetic/clean` (10 images)
- `data/synthetic/medium` (10 images)
- `data/synthetic/poor` (10 images)
- `data/synthetic/ground_truth` (JSON per image)

### Requirements
- Synthetic/anonymized only (no real personal data)
- Ground truth schema must include the required fields

### Acceptance Criteria
- Exactly 30 images categorized 10/10/10
- Ground truth exists for every image

---

## T18 - Evaluation harness + OCR engine comparison report

### Goal
Generate a repeatable report comparing OCR engines and performance.

### Context
POC success requires measurable outcomes:
- = 85% required-field accuracy
- = 10s median scan-form time
- Comparison across engines (paddle/vision/Vision if enabled)

### Task
Create an evaluation runner that:
- iterates the dataset buckets
- runs scanning per engine
- computes:
  - per-field accuracy
  - overall required-field accuracy
  - median and p95 scan-form time
  - % scans with confidence < 0.70
- outputs a markdown report:
  - per engine
  - per quality bucket
  - top failure patterns

### Requirements
- Must be repeatable locally
- Must not log OCR text or PII
- Cloud engines only run if enabled

### Acceptance Criteria
- Running evaluation generates a markdown report with tables
- Report includes pass/fail vs thresholds

---

## T19 - Staging deployment + security/privacy checklist verification

### Goal
Deploy the POC to staging and verify privacy guarantees.

### Context
The POC brief requires HTTPS staging and a completed security/privacy checklist artifact.

### Task
1) Create/update a checklist doc (e.g., `docs/security-checklist.md`) that confirms:
- No image persistence (default)
- No OCR text logged
- No parsed fields logged
- `Cache-Control: no-store`
- HTTPS in staging
- OCR worker protected (internal key or private service)

2) Deploy to staging (or prepare deployment config):
- Web (HTTPS)
- API (HTTPS)
- OCR worker (HTTPS)

### Requirements
- Do not commit secrets/keys
- Ensure staging is HTTPS-only

### Acceptance Criteria
- Checklist exists and is filled in (not blank)
- Staging endpoints work end-to-end over HTTPS
- Manual verification confirms no PII in logs and no persistence

---
