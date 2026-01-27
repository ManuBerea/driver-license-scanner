# Gate 1 — Requirements & Acceptance

This document contains a set of **15 user stories** with **testable acceptance criteria**.

---

## Story 01 — Capture or upload a driving licence image

**As a** motor trade staff member  
**I want** to capture a UK driving licence photo via webcam or upload a file  
**So that** I can start the scan process without manual typing.

**Acceptance Criteria**
- UI supports **webcam capture** and **file upload**
- Supported formats: **.jpg, .jpeg, .png, .webp**
- Unsupported format shows a **clear error message** (no crash)
- **Max file size limit** enforced (e.g., **5–10MB**) with error message
- User can proceed only after selecting/capturing an image

---

## Story 02 — Preview the image and retry before scanning

**As a** motor trade staff member  
**I want** to preview the captured/uploaded image and retry  
**So that** I can improve OCR results before scanning.

**Acceptance Criteria**
- Preview screen shows the image clearly
- User can **Retake** or **Choose another** image
- **Scan** button triggers backend request
- Preview does **not** upload automatically until user clicks **Scan**

---

## Story 03 — Submit image to backend scan endpoint

**As a** motor trade staff member  
**I want** the app to send the image securely to the backend  
**So that** OCR can run on the server and return results.

**Acceptance Criteria**
- Frontend sends image via **HTTPS** to `POST /ocr/scan` (or equivalent API route)
- UI shows a **loading state** until the response is received
- API returns JSON with:
  - `requestId`
  - `ocrConfidence` (0–1)
  - `rawOcr` (optional in MVP) **OR** parsed fields
- Failure responses show a **clear error message** and allow retry
- Backend response time is captured (backend metric)
- Median time from capture → populated form **≤ 10 seconds**
- If request exceeds timeout, return `OCR_TIMEOUT` error code and user can retry
- No image is stored on the frontend beyond the session
- Confirm **no image bytes or OCR text is logged** anywhere

---

## Story 04 — Run server-side OCR using PaddleOCR

**As a** system  
**I want** to run OCR using PaddleOCR (primary) on license images  
**So that** text can be extracted reliably from photos (not scans only).

**Acceptance Criteria**
- FastAPI OCR worker accepts multipart image upload
- OCR worker returns:
  - list of text lines (words) and bounding boxes (if available)
  - an overall confidence score (0–1)
- OCR worker does **not persist** the image to disk by default
- OCR worker exposes `/health` returning **200 OK**

---

## Story 05 — Orchestrate OCR calls from Spring Boot API

**As a** developer  
**I want** Spring Boot API to call the OCR worker internally  
**So that** the frontend only communicates with one API and internal services stay protected.

**Acceptance Criteria**
- Spring API calls OCR worker using internal endpoint + auth header (e.g. `X-INTERNAL-KEY`)
- If OCR worker is unavailable, API returns a **user-friendly error payload**
- API does **not** log raw images, OCR text, or extracted PII
- API returns standardized error codes (e.g. `OCR_TIMEOUT`, `OCR_FAILED`)

---

## Story 06 — OCR engine abstraction + config (Paddle/Vision)

**As a** developer  
**I want** a pluggable OCR engine abstraction  

**Notes**
- Primary engine: **PaddleOCR**
- All external services behind flags (safe defaults)

**Acceptance Criteria**
- OCR engines implement a common interface (e.g. `run(image) -> OcrResult`)
- `OcrResult` includes:
  - `lines/words`
  - `boundingBoxes` (if available)
  - `overallConfidence` (0–1)
  - `engineName`
  - `processingTimeMs`
  - `perFieldConfidence` (optional, later)
- Spring Boot can request OCR using `OCR_ENGINE=paddle|vision`
- Default engine is **PaddleOCR**
- Vision require API keys and are **OFF by default** via flags
- Each engine call records **timing and confidence metadata only** (no PII)

---

## Story 07 — Confidence-based fallback orchestration (threshold trigger)

**As a** motor trade staff member  
**I want** the system to automatically retry OCR with fallback engines when confidence is low  
**So that** I get the best possible auto-fill results from a photo scan.

**Fallback order**
1. PaddleOCR (primary)
3. Google Vision (if enabled)

**Trigger**
- overall confidence < threshold (e.g. < 0.70), **OR**
- parsing failed, **OR**
- required fields are missing

**Acceptance Criteria**
- System runs PaddleOCR first
- If trigger condition is met, system tries Vision next
- If still below threshold or parsing still fails, tries Google Vision (if enabled)
- Final response includes:
  - `selectedEngine`
  - `attemptedEngines[]`
  - `processingTimeMs`
  - `ocrConfidenceOverall`
  - parsed fields
  - `warnings[]` (e.g. `"LOW_CONFIDENCE"`, `"FALLBACK_USED"`)
- Low confidence always triggers a warning in UI when < 0.70
- No raw images stored and no PII logged during fallback attempts

---

## Story 08 — Parse OCR output into structured driver fields

**As a** motor trade staff member  
**I want** the system to extract structured fields from OCR output  
**So that** the form can be auto-filled with usable driver information.

**Fields to extract**
- First name
- Last name
- Date of birth
- Address line (includes postcode)
- Licence number
- Expiry date
- (Optional) licence categories

**Acceptance Criteria**
- Parser returns structured JSON with expected field names
- Parser normalizes:
  - whitespace
  - date formats into consistent UK DD.MM.YYYY
  - uppercase/lowercase where needed
- If a field cannot be found, returns **null or empty string** (no guessing)
- Parser never throws unhandled exceptions (safe failure)

---

## Story 09 — Auto-fill an editable driver form

**As a** motor trade staff member  
**I want** the app to auto-fill a driver form with extracted values  
**So that** I can review and correct details quickly.

**Acceptance Criteria**
- Form renders all required fields as editable inputs
- Auto-filled fields can be edited and overwritten
- Missing fields are visually highlighted as **Required**
- User can manually fill missing values
- Autofill happens within the same page/session (no refresh required)
- When `ocrConfidence ≥ 0.70`, **at least 5 required fields** are auto-filled correctly on **clean/medium synthetic inputs**

---

## Story 10 — Warn when OCR confidence is below the threshold

**As a** motor trade staff member  
**I want** a clear warning when OCR confidence is below 70%  
**So that** I know results may be inaccurate and must be reviewed.

**Acceptance Criteria**
- If `ocrConfidence < 0.70`, UI shows warning banner
- Warning explains: **“Low confidence — please review fields”**
- Warning shown consistently for all low-confidence responses
- Confidence threshold is configurable via env var (backend)

---

## Story 11 — Validate driver fields before allowing save

**As a** motor trade staff member  
**I want** driver details validated before submission  
**So that** invalid or incomplete driver profiles are prevented.

**Validation rules**
- Licence expiry blocks submission
- Required fields must be present
- UK postcode in address line must be valid format
- UK licence number must match format
- Age rule: outside 21–75 → warning

**Acceptance Criteria**
- Submission is blocked if any required field is missing
- Submission is blocked if licence is expired
- Invalid postcode in address line blocks submission with clear error message
- Invalid licence number blocks submission with clear error message
- Age rule behavior implemented consistently (warn or block per definition)
- Validation errors are shown next to relevant fields

---

## Story 12 — Handle failures with clear messaging and retry options

**As a** motor trade staff member  
**I want** clear error handling when scanning fails  
**So that** I can retry or recover without being blocked.

**Acceptance Criteria**
- OCR failures produce a readable message (not stack traces)
- User can retry scanning without refreshing the page
- Errors include a non-sensitive error code (e.g. `OCR_FAILED`)
- No PII included in errors or logs

---

## Story 13 — Enforce privacy-safe processing end-to-end

**As a** compliance-minded stakeholder  
**I want** the POC to be privacy-safe by design  
**So that** no licence images or PII are stored or leaked.

**Acceptance Criteria**
- Raw licence images are not stored by default (in-memory only)
- Logs do not contain OCR text, parsed values, or images
- Responses include `Cache-Control: no-store`
- OCR worker is protected (private or requires `X-INTERNAL-KEY`)
- HTTPS is enabled on staging environment
- Security checklist is completed and included in repo

---

## Story 14 — OCR engine comparison report (Paddle vs Vision)

**As a** stakeholder  
**I want** a comparison report across OCR engines  
**So that** we understand which OCR approach is best for UK licence photos under different quality conditions.

**Engines**
- PaddleOCR
- Google Vision OCR

**Acceptance Criteria**
- Synthetic dataset contains ≥ 30 images (10 clean / 10 medium / 10 poor)
- For each engine, measure:
  - per-field accuracy
  - overall required-field accuracy
  - median processing time
  - confidence distribution
  - p95 processing time
  - median time by quality bucket (clean/medium/poor)
- Report contains pass/fail statement against:
  - ≥ 85% required-field accuracy
  - median ≤ 10 seconds
- Report includes:
  - table of results by engine
  - results by quality bucket
  - top failure patterns per engine
- Report lives in `reports/ocr_engine_comparison.md`

---

## Story 15 (Stretch) — Parallel OCR run + per-field confidence merging

**As a** motor trade staff member  
**I want** the system to combine the best field values from multiple OCR engines when confidence is low  
**So that** the final auto-filled form is more accurate than any single OCR engine.

**Approach**
- Run multiple OCR engines (parallel or sequential)
- Parse each engine’s fields
- For each field, select the value with the highest confidence / best validation score

**Acceptance Criteria**
- When enabled, system can run 2 engines (Paddle + Vision)
- For each field, a confidence score exists, or a proxy score is computed using validation strength
- Merged result selects the best candidate per field
- UI can optionally display “source engine per field” (or at least in debug output)
- Merging never bypasses business validation rules (expiry, required fields)
- Results include a debug summary (synthetic-only) showing which engine won each field
- Merging logic only runs when enabled via feature flag
