# Instructions for implementing tickets (Copilot + Junie)

## Purpose
Build a POC that captures/uploads a **UK driving license image**, runs **OCR**, **parses fields**, applies **backend validation**, and **auto-fills an editable driver form**. Keep scope tight and changes ticket-focused.

## Hard constraints (non-negotiable)
- **Synthetic/anonymized data only** (never use real license photos).
- **No raw images stored** (no DB, no disk persistence by default).
- **No PII in logs/errors/analytics**:
    - DO NOT log OCR text
    - DO NOT log parsed fields (name/DOB/address/license number)
- **HTTPS only** for staging.
- Scan responses must include `Cache-Control: no-store`.
- Keep code modular: OCR ≠ parsing ≠ validation.

Allowed to log: `requestId`, timings, engine name, confidence score, error codes.

## Scope (do not expand)
**In scope:** capture/upload → preview/retry → Scan → OCR → parse → validate → editable autofill form  
**Out of scope:** persistence, DVLA integration, auth, non-UK IDs

Supported inputs: `.jpg .jpeg .png .webp`, enforce 5–10MB size limit (UI + API).

## Required extracted fields (best-effort)
Required:
- `firstName`, `lastName`
- `dateOfBirth` (ISO `YYYY-MM-DD`)
- `addressLine`, `postcode`
- `licenceNumber`
- `expiryDate` (ISO `YYYY-MM-DD`)

Optional:
- `categories[]`

### Parser rules
- Deterministic regex/heuristics by default
- Normalize casing/whitespace/dates
- **No guessing**: unknowns must be null/empty
- Never crash on malformed OCR input

## Validation rules (backend authoritative)
Return `validation.blockingErrors[]` and `validation.warnings[]`

Blocking:
- missing required fields
- invalid UK postcode
- invalid UK license format
- expired license (`expiryDate` in the past)

Warning (not blocking):
- age outside 21–75

## Success behaviors (UI must support)
- Capture/upload/paste image → preview → user clicks **Scan** (no auto-scan)
- Retry without refresh
- Editable autofill form
- If `ocrConfidence < 0.70`: show banner text exactly:
  **“Low confidence — please review fields”**
- Blocking errors shown next to fields; disable submit when any blocking errors exist

## Repo structure (3 apps)
- `web/` — Next.js UI
- `driver-license-scanner-api/` — Spring Boot API (orchestrates + parses + validates)
- `ocr-worker/` — FastAPI OCR worker

## Architecture flow
Web → API `POST /license/scan` → OCR Worker `POST /ocr` → API parse/validate → Web form

OCR worker protection:
- Internal network if possible; otherwise require `X-INTERNAL-KEY`.

## Feature flags (safe defaults)
- `OCR_ENGINE=paddle|doctr|vision|textract` (default: `paddle`)
- `ENABLE_FALLBACK_OCR=false`
- `OCR_CONFIDENCE_WARN_THRESHOLD=0.70`
- `ENABLE_LLM_CLEANUP=false` (off by default)

## API contracts (do not break)

### Web → API: `POST /license/scan` (multipart image)
Response must include:
- `requestId`
- `selectedEngine`, `attemptedEngines`
- `ocrConfidence`, `processingTimeMs`
- `fields` (nullable)
- `validation: { blockingErrors[], warnings[] }`

Headers: `Cache-Control: no-store`

Error response:
- Standard error codes: `INVALID_IMAGE`, `OCR_TIMEOUT`, `OCR_FAILED`, `PARSING_FAILED`

### API → OCR Worker: `POST /ocr` (multipart image + `X-INTERNAL-KEY`)
Worker response includes:
- `requestId`, `engine`, `confidence`, `lines[]`, `processingTimeMs`

## API internal components (match architecture diagrams)
Inside `driver-license-scanner-api/` keep these roles:
- **ScanController**: validates request (file type/size) + shapes HTTP response
- **ScanService**: orchestrates pipeline (OCR → parse → validate → optional fallback)
- **OcrClient**: calls OCR worker with timeout + maps worker errors to API error codes
- **ResponseAssembler**: builds final JSON response
- **FallbackOrchestrator** (flagged): retries OCR engines in a bounded way

### Bounded fallback attempts (prevent retry loops)
If `ENABLE_FALLBACK_OCR=true`, retries are limited:
- `MAX_FALLBACK_ATTEMPTS=2` (default)
- Fallback order: `paddle → doctr → vision → textract`
- Stop early if required fields are filled OR confidence ≥ threshold

## How the agent should work
- Implement only what the current ticket asks (no scope creep).
- Preserve module boundaries: OCR (worker) vs parser/validator (API).
- Never add PII logging or storage.
- Keep responses/contracts stable.
- Add/update tests when the ticket touches parser/validator/API contract; otherwise keep minimal.
- Include quick verification steps (curl/endpoint/UI steps).
- Stop once acceptance criteria are satisfied.
