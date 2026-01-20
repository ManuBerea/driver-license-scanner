# Codex + GitHub Copilot — Standardized Ticket Prompts

Each prompt includes:
- Objective
- Constraints
- Task
- Deliverables
- Verification
- Stop condition

---

## Prompt-T1 — Create monorepo baseline
```md
You are a coding agent working inside a monorepo at `driver-license-scanner/`.

## Objective
Set up a clean monorepo for 3 services

## Constraints (must follow)
- Do NOT commit secrets (`.env`) or generated folders (`.venv`, `node_modules`, build outputs)
- Keep changes minimal and only what is needed for this ticket

## Task
1) Ensure these folders exist at repo root:
- `web/`
- `driver-license-scanner-api/`
- `ocr-worker/`
- `docs/`

2) Create/Update `.gitignore` to ignore at minimum:
- `.env`
- `.venv/`
- `node_modules/`
- build outputs (e.g. `.next/`, `dist/`, `build/`, `target/`)
- python caches (`__pycache__/`, `.pytest_cache/`)

## Deliverables
- Updated/created `.gitignore`
- Folders present (`web/`, `driver-license-scanner-api/`, `ocr-worker/`)
- `docs/README.md` indexes gates

## Verification
- Show command output: `git status` (should not include venv/node_modules)

## Stop when
The repo structure + ignore rules are correct and verified.
```

---

## Prompt-T2 — Docker Compose: run all services locally
```md
You are a coding agent working inside a monorepo at `driver-license-scanner/`.

## Objective
Create `docker-compose.yml` so Web + API + OCR Worker run locally with one command.

## Constraints (must follow)
- No PII logs (do not add logging of OCR text or extracted fields)
- Keep configuration simple and dev-friendly
- Expose ports: web 3000, api 8080, worker 8000

## Task
Create root `docker-compose.yml` with 3 services:
- `web` -> port 3000
- `api` -> port 8080
- `ocr-worker` -> port 8000

Also ensure:
- API has a health route (any one is fine: `/health` or `/actuator/health`)
- OCR worker has `/health` and `/docs`

## Deliverables
- `docker-compose.yml` at repo root
- Any small required service Dockerfiles if missing (minimal)
- Health endpoints exist in API and worker

## Verification
- Provide exact command to run: `docker compose up --build`
- List expected URLs:
  - http://localhost:3000
  - http://localhost:8080 (health route returns 200)
  - http://localhost:8000/health and /docs

## Stop when
`docker compose up --build` boots all services and endpoints are reachable.
```

---

## Prompt-T3 — Web: capture/upload licence image
```md
You are a coding agent working inside `web/` of a monorepo.

## Objective
Create a UI component that lets the user upload an image or capture from webcam.

## Constraints (must follow)
- Allowed formats: .jpg .jpeg .png .webp
- Max file size: 5–10MB (choose a value and enforce it)
- Do NOT store the image permanently (memory/state only)
- Show user-friendly errors (no stack traces)

## Task
Implement a capture/upload UI that:
- supports file upload
- supports webcam capture (getUserMedia)
- validates type + size
- stores selected image in component state
- provides a "Continue" action only when an image exists

## Deliverables
- One React component implementing this functionality
- Minimal supporting utilities if needed

## Verification
- List validation rules
- Explain how the component signals “image selected”
- No persistence or localStorage usage

## Stop when
The component supports upload + webcam capture + validation + continue flow.
```

---

## Prompt-T4 — Web: preview + retry before scan
```md
You are a coding agent working inside `web/`.

## Objective
Add an image preview step with retry options before the scan request.

## Constraints (must follow)
- Do not upload automatically; only upload on user click
- Must allow retry without page refresh

## Task
Implement a preview UI that:
- shows the selected image preview
- provides retry controls (“Choose another” / “Retake”)
- provides a “Scan” button that triggers the scan flow

## Deliverables
- A preview component or integrated screen
- Clear events/props/state transitions (image -> preview -> scan)

## Verification
- Confirm Scan triggers request only when clicked
- Confirm retry replaces image correctly

## Stop when
Preview and retry work correctly and Scan is clickable only with an image.
```

---

## Prompt-T5 — API: `/license/scan` endpoint stub
```md
You are a coding agent working inside `driver-license-scanner-api/`.

## Objective
Create a scan endpoint that accepts an image upload and returns the expected response contract (stubbed).

## Constraints (must follow)
- Do NOT log image bytes
- Do NOT log OCR text or extracted fields
- Must include response header: Cache-Control: no-store
- Validate allowed formats: jpg/jpeg/png/webp

## Task
Implement `POST /license/scan`:
- Accept multipart/form-data field `image`
- Validate presence + allowed format
- Return JSON contract with empty/null `fields` (stub for now)
- Return standardized errors: INVALID_IMAGE, OCR_FAILED, OCR_TIMEOUT, PARSING_FAILED

## Deliverables
- Controller + DTOs needed
- Error response shape implemented
- Cache-Control header set

## Verification
- Provide example curl request + example responses
- Show where the header is set

## Stop when
Endpoint returns correct stub JSON and rejects invalid uploads with INVALID_IMAGE.
```

---

## Prompt-T6 — OCR worker: `/health` + `/ocr` stub
```md
You are a coding agent working inside `ocr-worker/`.

## Objective
Implement FastAPI endpoints /health and /ocr with a stub OCR response.

## Constraints (must follow)
- Do NOT persist uploaded files to disk
- Do NOT log OCR text or file contents (no PII logs)

## Task
Implement:
- GET /health -> 200 { "status": "ok" }
- POST /ocr accepts multipart field `image` and returns:
  { requestId, engine:"stub", confidence:0.0, lines:[], processingTimeMs:1 }

## Deliverables
- FastAPI app with routing
- Minimal models

## Verification
- Provide example curl to /health and /ocr
- Confirm no files are written to disk

## Stop when
Endpoints run and return correct JSON shapes.
```

---

## Prompt-T7 — Web: call scan API + loading/error/retry
```md
You are a coding agent working inside `web/`.

## Objective
Connect “Scan” to the backend, with loading and retry UX.

## Constraints (must follow)
- No stack traces shown to users
- Retry must work without page refresh

## Task
Implement:
- API client function that posts multipart to `/license/scan`
- Loading state during request
- Error UI with retry
- On success, store response for the next screen (form)

## Deliverables
- API client module
- UI wiring from Scan button

## Verification
- Explain the state transitions: idle -> loading -> success/error
- Confirm retry triggers another request

## Stop when
Scan request works with proper UX behavior.
```

---

## Prompt-T8 — v0 requirement: surface raw OCR output end-to-end
```md
You are a coding agent working in the monorepo.

## Objective
Ensure v0 demonstrates “capture + raw OCR output” end-to-end (even if stubbed).

## Constraints (must follow)
- Do NOT log OCR text server-side
- Raw OCR should be visible to the user only via response/UI (dev-friendly)

## Task
Implement one simple solution:
Option A: Include OCR lines in API scan response as a dev-only field, OR
Option B: UI shows OCR lines returned by API in a debug panel.

## Deliverables
- Updated API response and/or web debug UI

## Verification
- Demonstrate where raw OCR lines appear after scan

## Stop when
After Scan, raw OCR output is visible somewhere end-to-end.
```

---

## Prompt-T9 — OCR Worker: PaddleOCR integration
```md
You are a coding agent working inside `ocr-worker/`.

## Objective
Replace stub OCR with PaddleOCR and return real lines + confidence.

## Constraints (must follow)
- No persistence (memory only)
- No PII logs
- Response must include: requestId, engine, confidence 0..1, lines[], processingTimeMs

## Task
Implement PaddleOCR engine:
- Extract text lines (and per-line confidences)
- Compute overall confidence (0..1)
- Include processing time

## Deliverables
- PaddleOCR engine implementation
- /ocr uses PaddleOCR when configured

## Verification
- Show sample output structure (not real PII)
- Explain confidence calculation method

## Stop when
/ocr returns non-empty lines for clean synthetic input.
```

---

## Prompt-T10 — OCR engine selection abstraction (+ docTR + cloud placeholders)
```md
You are a coding agent working inside `ocr-worker/`.

## Objective
Implement OCR engine selection: paddle|doctr|vision|textract.

## Constraints (must follow)
- Default engine is paddle
- vision/textract must be disabled by default (explicit enable flags)
- If cloud engine selected without keys -> fail gracefully with OCR_FAILED
- No PII logs

## Task
Implement:
- Engine interface
- Config/env selection: OCR_ENGINE=paddle|doctr|vision|textract
- docTR engine implementation (local)
- vision/textract placeholders behind flags

## Deliverables
- Engine abstraction + selection
- docTR implementation
- graceful error behavior for cloud engines

## Verification
- Explain env vars and default behavior
- Confirm selecting doctr works and selecting vision fails gracefully without config

## Stop when
Engine selection works and all engines share the same output contract shape.
```

---

## Prompt-T11 — API: OCR client calls worker (internal)
```md
You are a coding agent working inside `driver-license-scanner-api/`.

## Objective
Make the API call OCR worker safely and handle failures.

## Constraints (must follow)
- No PII logs (no OCR text or parsed fields logged)
- Timeout failures must map to OCR_TIMEOUT, other failures to OCR_FAILED

## Task
Implement OCR worker client call:
- base URL from OCR_WORKER_URL env
- internal header X-INTERNAL-KEY
- timeout + error mapping

## Deliverables
- OCR client class/service
- Error handling mapping

## Verification
- Show example of success path and failure path handling

## Stop when
API can call worker and returns standardized errors on failures.
```

---

## Prompt-T12 — API: deterministic parser to required fields
```md
You are a coding agent working inside `driver-license-scanner-api/`.

## Objective
Parse OCR lines into structured fields deterministically.

## Constraints (must follow)
- No guessing: unknown fields must be null/empty
- Normalize dates to YYYY-MM-DD
- Must never crash the request

## Task
Implement a parser that extracts:
- firstName, lastName, dateOfBirth
- addressLine, postcode
- licenceNumber, expiryDate
Optional: categories

## Deliverables
- Parser implementation
- Parsing unit test scaffolding optional (full tests are later)

## Verification
- Provide examples of input OCR lines and expected parsed output (synthetic)

## Stop when
Parser returns best-effort structured fields and leaves unknowns empty.
```

---

## Prompt-T13 — API: scan response contract (fields + validation + headers)
```md
You are a coding agent working inside `driver-license-scanner-api/`.

## Objective
Return the final scan response contract to the frontend.

## Constraints (must follow)
- Cache-Control: no-store is required
- Do NOT return raw OCR text unless explicitly in dev/debug mode
- No PII logs

## Task
Update POST /license/scan to:
- call OCR worker
- parse fields
- return contract:
  requestId, selectedEngine, attemptedEngines, ocrConfidence, processingTimeMs, fields, validation

## Deliverables
- Updated controller/service wiring
- Contract DTOs

## Verification
- Provide sample JSON response (synthetic)
- Confirm header set

## Stop when
Scan endpoint returns correct contract and headers reliably.
```

---

## Prompt-T14 — Web: editable driver form + low-confidence warning
```md
You are a coding agent working inside `web/`.

## Objective
Show editable form auto-filled with scan response + low confidence warning.

## Constraints (must follow)
- Warning text must be exactly:
  “Low confidence — please review fields”
- Threshold must be configurable (do not hardcode 0.70 in UI)
- Fields must be editable

## Task
Implement a form UI for fields:
- firstName, lastName, dateOfBirth, addressLine, postcode, licenceNumber, expiryDate
Populate from API response and allow user edits.
Show warning banner if confidence below threshold.

## Deliverables
- Form UI component + warning UI
- Handling of missing fields

## Verification
- Explain how threshold is configured
- Show how missing required fields are highlighted

## Stop when
Form renders, autofills, is editable, and warning works.
```

---

## Prompt-T15 — API: backend validation rules (blocking + warnings)
```md
You are a coding agent working inside `driver-license-scanner-api/`.

## Objective
Validate extracted fields and return structured validation results.

## Constraints (must follow)
- Provide blockingErrors[] with field/code/message
- Provide warnings[]
- No PII logs

## Task
Implement validations:
- expiry in past => blocking
- missing required fields => blocking
- invalid UK postcode => blocking
- invalid licence number => blocking
- age outside 21–75 => warning

## Deliverables
- Validator implementation
- ValidationResult model

## Verification
- Provide examples of validation outputs (synthetic)

## Stop when
Validation returns correct blocking + warnings for common invalid cases.
```

---

## Prompt-T16 — Web: field-level errors + block submit
```md
You are a coding agent working inside `web/`.

## Objective
Render API validation errors next to fields and block submission when invalid.

## Constraints (must follow)
- User must still be able to edit fields even when errors exist

## Task
Update the form UI to:
- show blockingErrors next to matching field inputs
- show warnings in a banner/list
- disable submit/save while blocking errors exist

## Deliverables
- Updated form component behavior

## Verification
- Show how errors map to fields
- Demonstrate submit disabled state logic

## Stop when
Field errors render correctly and submit is blocked when invalid.
```

---

## Prompt-T17 — API: fallback OCR orchestration (optional, flagged)
```md
You are a coding agent working inside `driver-license-scanner-api/`.

## Objective
Implement optional OCR fallback strategy when results are low quality.

## Constraints (must follow)
- Must be OFF by default (ENABLE_FALLBACK_OCR=false)
- Must keep latency bounded
- attemptedEngines and selectedEngine must reflect behavior
- No PII logs

## Task
Implement fallback logic triggered when:
- ocrConfidence < 0.70 OR
- required fields missing after parsing OR
- OCR fails/timeouts

Fallback order: paddle -> doctr -> vision -> textract

## Deliverables
- Orchestrator changes to attempt fallback
- Response includes attemptedEngines[] and selectedEngine

## Verification
- Demonstrate behavior with fallback disabled vs enabled

## Stop when
Fallback works safely and metadata reflects attempts.
```

---

## Prompt-T18 — Synthetic dataset (30 images) + ground truth
```md
You are a coding agent working in the monorepo.

## Objective
Create a synthetic dataset structure for evaluation (no real PII).

## Constraints (must follow)
- Must be synthetic/anonymized only
- 10 clean / 10 medium / 10 poor
- Ground truth JSON must exist per image

## Task
Create folder structure:
- data/synthetic/clean
- data/synthetic/medium
- data/synthetic/poor
- data/synthetic/ground_truth

Create ground truth schema examples (JSON), and placeholders if images are not available yet.

## Deliverables
- Folder structure committed
- Ground truth JSON templates or examples

## Verification
- Confirm counts and schema completeness

## Stop when
Dataset structure exists and ground truth schema is ready for 30 items.
```

---

## Prompt-T19 — Evaluation harness + OCR comparison report
```md
You are a coding agent working in the monorepo.

## Objective
Build an evaluation runner that generates an OCR comparison report.

## Constraints (must follow)
- Must be repeatable locally
- Must not log OCR text or PII
- Cloud engines only run if enabled

## Task
Create a runner that:
- iterates dataset buckets
- runs scans per engine
- computes accuracy + timing metrics:
  per-field accuracy, overall required-field accuracy,
  median + p95 scan→form time, % confidence < 0.70
- writes a markdown report with tables and pass/fail vs thresholds

## Deliverables
- Evaluation script/tool
- Generated report template output file path

## Verification
- Explain how to run it and what it outputs

## Stop when
Running the harness produces a markdown report with metrics tables.
```

---

## Prompt-T20 — Staging deployment + security/privacy checklist verification
```md
You are a coding agent working in the monorepo.

## Objective
Prepare staging deployment and verify privacy/security requirements.

## Constraints (must follow)
- HTTPS only
- No secrets committed
- No PII logs or persistence

## Task
1) Create `docs/security-checklist.md` including checks:
- no image persistence
- no OCR text logs
- no parsed field logs
- Cache-Control: no-store
- HTTPS staging
- OCR worker protected (internal key/private)

2) Prepare deployment config/notes for:
- Web
- API
- OCR worker

## Deliverables
- Completed `docs/security-checklist.md`
- Deployment notes/config files (no secrets)

## Verification
- Checklist is filled in (not blank)
- Staging plan indicates HTTPS-only requirement

## Stop when
Checklist exists and staging deployment requirements are documented.
```
