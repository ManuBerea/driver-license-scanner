# Plan: Implement Driver License Scanner POC (v0 → v0.1 → v1)

Build a privacy-safe UK driving license OCR POC across 3 apps (Next.js Web, Spring Boot API, FastAPI OCR Worker) following strict PII guardrails. Implementation follows 20 tickets across 3 milestones (v0: capture + raw OCR; v0.1: parsing + autofill; v1: validation + tests + deploy), targeting ≥85% field accuracy and ≤10s scan time.

## Implementation Steps

### Step 1: Establish monorepo foundation and local dev environment
**Tickets: T1, T2**

Create root `docker-compose.yml` and update `.gitignore` to prevent secrets/build artifacts. Wire up health endpoints in `DriverLicenseScannerApiApplication.java` and `main.py`. Ensure all 3 services boot with `docker compose up --build`.

**Actions:**
- Create `docker-compose.yml` with 3 services: web (port 3000), api (port 8080), ocr-worker (port 8000)
- Update root `.gitignore` to ignore `.env`, `.venv`, `node_modules`, build outputs
- Add health endpoint to Spring Boot API at `/actuator/health`
- Implement `/health` endpoint in FastAPI OCR worker returning `{"status": "ok"}`
- Create Dockerfiles for all 3 services
- Test local startup with `docker compose up --build`

**Deliverables:**
- `docker-compose.yml`
- `.gitignore` (updated)
- Health endpoints functional
- All services accessible locally

---

### Step 2: Implement Web capture/upload flow with preview
**Tickets: T3, T4, T7**

Build image capture UI in `web/src/app/` with webcam/upload/paste support, file validation (jpg/jpeg/png/webp, 5-10MB), preview screen with retry, and **Scan** button using React Hook Form + Zod. Wire `POST /license/scan` API call with loading/error/retry states.

**Actions:**
- Create image capture component supporting:
  - File upload (drag & drop + file input)
  - Webcam capture using `getUserMedia`
  - Paste from clipboard
- Implement client-side validation:
  - File type: `.jpg`, `.jpeg`, `.png`, `.webp`
  - File size: 5-10MB limit
  - Clear error messages for invalid inputs
- Build preview screen with:
  - Image preview display
  - "Choose another" / "Retake" buttons
  - "Scan" button (manual trigger, no auto-scan)
- Implement API integration:
  - `POST /license/scan` with multipart form data
  - Loading state during request
  - Error handling with user-friendly messages
  - Retry capability without page refresh
- Store image in component state only (no persistence)

**Deliverables:**
- Capture/upload UI component
- Preview screen component
- API integration with error handling
- No automatic upload until user clicks "Scan"

---

### Step 3: Build API scan orchestration pipeline
**Tickets: T5, T11, T13**

Create `ScanController` in `controller/` accepting multipart image, `ScanService` in `service/` orchestrating OCR→parse→validate, `OcrClient` in `ocr/` calling worker with `X-INTERNAL-KEY`, return contract with `Cache-Control: no-store`.

**Actions:**
- Create `ScanController.java`:
  - `POST /license/scan` endpoint
  - Accept multipart file field `image`
  - Validate file presence, type, and size
  - Return standardized error codes: `INVALID_IMAGE`, `OCR_TIMEOUT`, `OCR_FAILED`, `PARSING_FAILED`
  - Set `Cache-Control: no-store` header
- Create `ScanService.java`:
  - Orchestrate pipeline: validate → OCR → parse → validate → assemble response
  - Handle errors and map to user-friendly messages
- Create `OcrClient.java`:
  - Call OCR worker `POST /ocr` with image
  - Add `X-INTERNAL-KEY` header (from env var)
  - Implement timeout handling
  - Map worker errors to API error codes
  - Never log image bytes or OCR text
- Create response DTOs matching contract:
  - `ScanResponse` with: requestId, selectedEngine, attemptedEngines, ocrConfidence, processingTimeMs, fields, validation
  - Include `Cache-Control: no-store` in response headers
- Create `ResponseAssembler` to build final JSON response
- Add configuration for:
  - `OCR_WORKER_URL`
  - `X_INTERNAL_KEY`

**Deliverables:**
- `ScanController.java` with `/license/scan` endpoint
- `ScanService.java` orchestrating pipeline
- `OcrClient.java` calling worker
- Response DTOs matching API contract
- Error handling with standardized codes
- No PII in logs

---

### Step 4: Integrate PaddleOCR and engine abstraction in OCR Worker
**Tickets: T6, T9, T10**

Implement `/health` and `/ocr` endpoints in `ocr-worker/main.py`, create OCR engine interface with PaddleOCR primary implementation, placeholders for Vision behind `OCR_ENGINE` config, return `lines[]` with confidence and timing, protect with `X-INTERNAL-KEY`.

**Actions:**
- Update `main.py` with:
  - `GET /health` → `200 {"status": "ok"}`
  - `POST /ocr` accepting multipart `image` field
  - `X-INTERNAL-KEY` header validation
  - Return 401 if key missing/invalid
- Create OCR engine abstraction:
  - `OcrEngine` interface/base class with `run(image_bytes) -> OcrResult`
  - `OcrResult` dataclass: requestId, engine, confidence, lines[], processingTimeMs
  - `OcrLine` dataclass: text, confidence
- Implement PaddleOCR engine:
  - Install `paddleocr` dependency
  - Extract text lines with bounding boxes
  - Calculate overall confidence (average of line confidences)
  - Track processing time
  - Same interface as PaddleOCR
  - Calculate confidence and timing
- Add placeholders for cloud engines:
  - `GoogleVisionEngine` (requires `ENABLE_VISION_OCR=true` + credentials)
  - Fail gracefully with `OCR_FAILED` if selected without credentials
- Add configuration via env vars:
  - `OCR_ENGINE=paddle|vision` (default: `paddle`)
  - `X_INTERNAL_KEY` (required)
  - `ENABLE_VISION_OCR=false`
- Never persist images to disk
- Never log image bytes or OCR text (only requestId, engine, confidence, timing)

**Deliverables:**
- `/health` and `/ocr` endpoints
- OCR engine abstraction (interface + PaddleOCR implementations)
- Cloud engine placeholders (Vision)
- Configuration via env vars
- Internal auth header protection
- No image persistence, no PII logs

---

### Step 5: Build deterministic parser and validator modules
**Tickets: T12, T15**

Create `UkLicenseParser` in `parser/` extracting firstName/lastName/dateOfBirth/addressLine (includes postcode)/licenceNumber/expiryDate using regex/heuristics with date normalization, create `UkLicenseValidator` in `validator/` implementing blocking errors (expired/missing fields/invalid postcode in address line/license format) and warnings (age 21-75), write JUnit tests for both.

**Actions:**
- Create `UkLicenseParser.java`:
  - `parse(List<OcrLine> lines) -> LicenseFields`
  - Extract required fields using deterministic regex/heuristics:
    - firstName, lastName (common patterns on UK licenses)
    - dateOfBirth (DD/MM/YYYY or similar formats)
    - addressLine (includes postcode)
    - licenceNumber (UK license format: AAAAA######A##AA)
    - expiryDate (date formats)
    - categories (optional, e.g., "A", "B", "C")
  - Normalize:
    - Whitespace (trim, collapse multiple spaces)
    - Dates to UK format `DD.MM.YYYY`
    - Casing where appropriate (uppercase for license number)
  - Return null/empty for unknown fields (no guessing)
  - Never throw unhandled exceptions (safe failure)
- Create `LicenseFields.java` DTO:
  - All fields nullable/optional
  - Lombok `@Data` annotation
- Create `UkLicenseValidator.java`:
  - `validate(LicenseFields fields) -> ValidationResult`
  - Blocking errors:
    - Missing required fields (firstName, lastName, dateOfBirth, addressLine, licenceNumber, expiryDate)
    - Expired license (expiryDate in the past)
    - Invalid UK postcode format in address line
    - Invalid UK license number format
  - Warnings:
    - Age outside 21-75 range (calculate from dateOfBirth)
    - Low confidence (handled by service, but validator can flag if needed)
- Create `ValidationResult.java` DTO:
  - `blockingErrors: List<ValidationError>`
  - `warnings: List<ValidationWarning>`
  - `ValidationError`: field, code, message
  - `ValidationWarning`: code, message
- Write JUnit tests:
  - Parser tests: typical OCR input → expected fields
  - Parser tests: malformed input → safe handling
  - Parser tests: date normalization
  - Validator tests: each blocking rule
  - Validator tests: each warning rule
  - Validator tests: multiple errors/warnings

**Deliverables:**
- `UkLicenseParser.java` with deterministic parsing
- `UkLicenseValidator.java` with all rules
- DTOs: `LicenseFields`, `ValidationResult`, `ValidationError`, `ValidationWarning`
- Comprehensive JUnit tests (>80% coverage)
- No guessing, safe failure handling

---

### Step 6: Implement editable autofill form with validation UI
**Tickets: T14, T16**

Create driver form component in `web/src/app/` auto-filling from API response, display low confidence warning banner when `ocrConfidence < 0.70`, show field-level `blockingErrors`, disable submit when blocking errors exist, allow manual field editing.

**Actions:**
- Create driver form component:
  - Use React Hook Form for form state management
  - Use Zod for schema validation
  - Fields: firstName, lastName, dateOfBirth, addressLine, licenceNumber, expiryDate, categories
  - All fields editable (text inputs)
  - Populate fields from API `fields` response
  - Mark required fields visually
- Implement low confidence warning:
  - Show banner when `response.ocrConfidence < 0.70`
  - Banner text: **"Low confidence — please review fields"**
  - Prominent styling (yellow/orange background)
- Implement validation error display:
  - Map `response.validation.blockingErrors` to form fields
  - Show field-level error messages next to inputs
  - Highlight invalid fields (red border)
  - Display non-blocking warnings in a separate section
- Implement submit logic:
  - Disable submit button when any blocking errors exist
  - Enable submit when all blocking errors resolved
  - Handle form submission (for POC: just log/show success)
- Styling with Tailwind CSS:
  - Clean, accessible form layout
  - Clear error states
  - Responsive design
- Handle edge cases:
  - Partial API response (some fields missing)
  - Empty fields remain editable
  - User can override any auto-filled value

**Deliverables:**
- Editable driver form component
- Auto-fill from API response
- Low confidence warning banner (<0.70)
- Field-level blocking error display
- Submit disabled when blocking errors exist
- Clean, accessible UI

---

### Step 7: Add optional fallback OCR orchestration
**Tickets: T17**

Create `FallbackOrchestrator` in `service/` behind `ENABLE_FALLBACK_OCR=false` flag with bounded retry logic (paddl->vision) when confidence <0.70 or required fields missing, track `attemptedEngines[]` and `selectedEngine` in response, limit to `MAX_FALLBACK_ATTEMPTS=2`.

**Actions:**
- Create `FallbackOrchestrator.java`:
  - Check if fallback enabled via `ENABLE_FALLBACK_OCR` env var
  - If disabled, return after first OCR attempt
  - If enabled, implement bounded retry logic:
    - Fallback order: paddle -> vision
    - Max attempts: `MAX_FALLBACK_ATTEMPTS=2` (env var)
    - Trigger conditions:
      - `ocrConfidence < OCR_CONFIDENCE_WARN_THRESHOLD` (default 0.70)
      - Required fields missing after parsing
      - OCR timeout/failure
    - Stop early if:
      - All required fields populated AND confidence ≥ threshold
      - Max attempts reached
      - No more engines available
  - Track metadata:
    - `attemptedEngines: List<String>` (order of attempts)
    - `selectedEngine: String` (final engine that provided results)
    - Total processing time across attempts
- Update `ScanService`:
  - Integrate `FallbackOrchestrator`
  - Pass initial OCR result + parsed fields
  - Get final result with all metadata
- Update `ScanResponse`:
  - Include `attemptedEngines` array
  - Include `selectedEngine` string
- Add configuration:
  - `ENABLE_FALLBACK_OCR=false` (default off)
  - `MAX_FALLBACK_ATTEMPTS=2`
  - `OCR_CONFIDENCE_WARN_THRESHOLD=0.70`
- Logging:
  - Log engine attempts (no PII)
  - Log fallback triggers
  - Log total processing time

**Deliverables:**
- `FallbackOrchestrator.java` with bounded retry
- Feature flag: `ENABLE_FALLBACK_OCR=false`
- Metadata tracking: `attemptedEngines`, `selectedEngine`
- Integration with `ScanService`
- No PII logs

---

### Step 8: Create synthetic test dataset and evaluation harness
**Tickets: T18, T19**

Build `data/synthetic/` folders (clean/medium/poor with 10 images each) plus `ground_truth/` JSON files, create evaluation runner script comparing all OCR engines against ground truth, compute per-field accuracy, median/p95 timing, confidence distribution, generate markdown report with pass/fail vs ≥85% accuracy and ≤10s thresholds.

**Actions:**
- Create dataset structure:
  - `data/synthetic/clean/` (10 images)
  - `data/synthetic/medium/` (10 images)
  - `data/synthetic/poor/` (10 images)
  - `data/synthetic/ground_truth/` (30 JSON files, one per image)
- Generate/collect synthetic license images:
  - Create anonymized UK license templates
  - Clean: high resolution, good lighting, straight angle
  - Medium: slight blur, angle, or lighting issues
  - Poor: heavy blur, poor lighting, extreme angles
  - **Must be synthetic/anonymized** (no real PII)
- Create ground truth JSON schema:
  ```json
  {
    "imageFile": "clean_001.jpg",
    "fields": {
      "firstName": "JOHN",
      "lastName": "SMITH",
      "dateOfBirth": "13.01.1990",
      "addressLine": "1 HIGH STREET, SW1A 1AA",
      "licenceNumber": "SMITH901133J99AB",
      "expiryDate": "13.01.2030",
      "categories": ["A", "B"]
    }
  }
  ```
- Create evaluation script (Python or Java):
  - Iterate all images in dataset
  - For each OCR engine (paddle, vision if enabled):
    - Call `/license/scan` API
    - Compare extracted fields to ground truth
    - Track metrics per field:
      - Exact match
      - Partial match (fuzzy)
      - Missing
      - Incorrect
  - Compute aggregate metrics:
    - Per-field accuracy (% exact matches)
    - Overall required-field accuracy (all 7 required fields correct)
    - Median processing time (scan → form populated)
    - P95 processing time
    - Median time by quality bucket (clean/medium/poor)
    - % scans with confidence < 0.70
    - Common failure patterns (which fields fail most)
  - Generate markdown report:
    - Summary table: engine comparison
    - Per-bucket results (clean/medium/poor)
    - Per-field accuracy breakdown
    - Processing time statistics
    - Pass/fail vs success metrics:
      - ≥85% required-field accuracy
      - Median ≤10 seconds
    - Top failure patterns
- Save report to `reports/ocr_engine_comparison.md`

**Deliverables:**
- 30 synthetic license images (10/10/10 by quality)
- 30 ground truth JSON files
- Evaluation script/runner
- Generated comparison report
- Pass/fail statement vs success metrics

---

### Step 9: Deploy to staging and verify privacy checklist
**Tickets: T20**

Create Dockerfiles for API and OCR worker, configure Render deployment for both services + Vercel for web, enforce HTTPS, create `docs/security-checklist.md` confirming no image persistence, no PII logs, `Cache-Control: no-store`, protected OCR worker, perform end-to-end manual verification.

**Actions:**
- Create Dockerfiles:
  - `driver-license-scanner-api/Dockerfile`:
    - Multi-stage build (Gradle + JRE)
    - Expose port 8080
    - Health check on `/actuator/health`
  - `ocr-worker/Dockerfile`:
    - Python 3.11+ base image
    - Install PaddleOCR, dependencies
    - Expose port 8000
    - Health check on `/health`
  - `web/Dockerfile` (if needed for Render, otherwise Vercel handles):
    - Node.js build
    - Next.js static export or SSR
- Configure deployment:
  - **Web (Vercel)**:
    - Connect GitHub repo
    - Auto-deploy from `main` branch
    - Set env var: `NEXT_PUBLIC_API_URL` (API staging URL)
    - Enable HTTPS (automatic on Vercel)
  - **API (Render)**:
    - Create Web Service from Docker
    - Set env vars:
      - `OCR_WORKER_URL` (OCR worker internal URL)
      - `X_INTERNAL_KEY` (secret)
      - `ENABLE_FALLBACK_OCR=false`
      - `OCR_ENGINE=paddle`
      - `OCR_CONFIDENCE_WARN_THRESHOLD=0.70`
    - Health check path: `/actuator/health`
    - Enable HTTPS
  - **OCR Worker (Render)**:
    - Create Web Service from Docker
    - Set env vars:
      - `X_INTERNAL_KEY` (same secret as API)
      - `OCR_ENGINE=paddle`
    - Health check path: `/health`
    - Enable HTTPS (or keep internal if possible)
    - If public, ensure `X-INTERNAL-KEY` protection
- Create `docs/security-checklist.md`:
  - [ ] No image persistence (default behavior verified)
  - [ ] No OCR text logged (code review + log inspection)
  - [ ] No parsed fields logged (code review + log inspection)
  - [ ] `Cache-Control: no-store` header present (tested)
  - [ ] HTTPS enforced on all services (verified)
  - [ ] OCR worker protected (internal network or `X-INTERNAL-KEY` verified)
  - [ ] No PII in error messages (tested)
  - [ ] Synthetic dataset only (confirmed)
  - [ ] `.env` files not committed (verified)
  - [ ] Secrets in environment variables only (verified)
- Manual verification:
  - End-to-end flow on staging
  - Check network tab for HTTPS
  - Check response headers for `Cache-Control: no-store`
  - Verify no PII in browser console or network responses
  - Test error scenarios (invalid image, OCR failure)
  - Verify retry works
  - Test form autofill and validation

**Deliverables:**
- Dockerfiles for API and OCR worker
- Staging deployment (all 3 services over HTTPS)
- `docs/security-checklist.md` (completed)
- Manual verification complete
- End-to-end demo functional

---

## Architecture Summary

### System Components

1. **Web (Next.js + TypeScript + Tailwind CSS)**
   - Image capture/upload/paste
   - Preview + retry
   - API integration (`POST /license/scan`)
   - Editable driver form with validation UI
   - Low confidence warning banner

2. **API (Spring Boot 3 + Java 17)**
   - `ScanController`: request validation, response shaping
   - `ScanService`: pipeline orchestration
   - `OcrClient`: calls OCR worker
   - `UkLicenseParser`: deterministic field extraction
   - `UkLicenseValidator`: blocking errors + warnings
   - `FallbackOrchestrator`: optional bounded retries
   - `ResponseAssembler`: builds JSON response

3. **OCR Worker (FastAPI + Python)**
   - `/health` endpoint
   - `/ocr` endpoint (protected with `X-INTERNAL-KEY`)
   - OCR engine abstraction
   - Returns: lines[], confidence, timing

### Data Flow

```
User → Web (capture) → Preview → Scan button
  → API POST /license/scan
  → API validates file
  → API → OCR Worker POST /ocr (with X-INTERNAL-KEY)
  → OCR Worker runs engine (paddle/vision)
  → OCR Worker returns lines[] + confidence
  → API parses fields (UkLicenseParser)
  → API validates fields (UkLicenseValidator)
  → API (optional) FallbackOrchestrator retries if needed
  → API returns ScanResponse (fields + validation + metadata)
  → Web displays form (autofilled)
  → Web shows low confidence warning if < 0.70
  → Web shows blocking errors per field
  → Web disables submit if blocking errors exist
  → User edits fields → Submit
```

### PII Guardrails (Non-Negotiable)

1. **No image persistence**: Images kept in memory only during request
2. **No PII logs**: Never log OCR text, parsed fields, or image bytes
3. **Cache headers**: `Cache-Control: no-store` on all scan responses
4. **HTTPS only**: All staging services over HTTPS
5. **Protected OCR worker**: Internal network or `X-INTERNAL-KEY` auth
6. **Synthetic data only**: Test dataset is anonymized/synthetic

### Success Metrics

- **≥85% required-field accuracy** on synthetic dataset
- **Median scan→form time ≤10 seconds**
- **Confidence <70% triggers warning** consistently
- **Expired license blocks submission** reliably
- **Clear error messaging** for all failure cases

---

## Milestones

### v0 — Capture + Raw OCR Output
**Goal**: End-to-end flow with stub/early OCR response

- Docker compose setup (T1, T2)
- Web capture/upload/preview (T3, T4)
- API `/license/scan` stub (T5)
- OCR worker `/ocr` stub (T6)
- Web API integration (T7)
- Raw OCR output visible (T8)

**Exit Criteria**: User can scan image and see raw OCR lines (or stub) without crashes

---

### v0.1 — Parsed Fields + Form Autofill
**Goal**: Structured fields and populated form

- PaddleOCR integration (T9)
- OCR engine abstraction (T10)
- API→Worker integration (T11)
- Deterministic parser (T12)
- API response contract (T13)
- Editable form + low confidence warning (T14)

**Exit Criteria**: Clean/medium synthetic images populate form with usable values

---

### v1 — Validation + Tests + Deploy
**Goal**: Production-ready POC with metrics

- Backend validation (T15)
- Field-level errors + submit blocking (T16)
- Optional fallback orchestration (T17)
- Synthetic dataset (T18)
- Evaluation harness + comparison report (T19)
- Staging deployment + security checklist (T20)

**Exit Criteria**: 
- Meets success metrics (≥85% accuracy, ≤10s median)
- All guardrails satisfied
- Live staging demo ready

---

## Configuration & Feature Flags

### API Environment Variables
```
OCR_WORKER_URL=http://ocr-worker:8000
X_INTERNAL_KEY=<secret>
ENABLE_FALLBACK_OCR=false
MAX_FALLBACK_ATTEMPTS=2
OCR_CONFIDENCE_WARN_THRESHOLD=0.70
ENABLE_LLM_CLEANUP=false
```

### OCR Worker Environment Variables
```
X_INTERNAL_KEY=<secret>
OCR_ENGINE=paddle|vision
ENABLE_VISION_OCR=false
GOOGLE_APPLICATION_CREDENTIALS=/path/to/gcp-key.json (if vision enabled)
```

### Web Environment Variables
```
NEXT_PUBLIC_API_URL=https://api.staging.example.com
```

---

## Testing Strategy

### Automated Tests

1. **API (Spring Boot + JUnit 5)**
   - Parser unit tests (typical input, malformed input, date normalization)
   - Validator unit tests (each blocking rule, each warning rule, multiple errors)
   - OCR pipeline integration test (mocked OCR response, contract validation)
   - Target: >80% code coverage

2. **OCR Worker (Python + pytest)**
   - `/health` endpoint test
   - `/ocr` contract-shape test
   - Optional: PaddleOCR smoke test on one synthetic image

3. **Web (Next.js + TypeScript)**
   - Lint + build checks (minimum)
   - Optional: component tests (stretch)

### Manual Tests

1. **UX checks**
   - Capture/upload on desktop
   - Preview + retry flow
   - Form autofill + manual override
   - Error states and retry

2. **Performance checks**
   - Median time capture → form populated (target: ≤10s)
   - Test across dataset buckets (clean/medium/poor)

3. **Privacy/guardrails checks**
   - No OCR text in logs
   - No parsed fields in logs
   - No images persisted
   - `Cache-Control: no-store` header present
   - HTTPS on staging
   - `X-INTERNAL-KEY` protection working

---

## Risk Mitigation

### Risk 1: OCR accuracy below 85% threshold
**Mitigation**: 
- Implement fallback orchestration (T17)
- Test multiple engines (Paddle, Vision)
- Allow manual field correction in form
- Synthetic dataset includes quality variation

### Risk 2: Processing time exceeds 10s median
**Mitigation**:
- Optimize image preprocessing
- Use smaller OCR models if needed
- Implement timeouts to prevent hanging
- Bounded fallback attempts (max 2)
- Profile and optimize slow components

### Risk 3: PII leakage in logs or persistence
**Mitigation**:
- Code review for all log statements
- Explicit checks: never log `image`, `ocrText`, `fields`
- In-memory processing only (no disk writes)
- Security checklist verification (T20)
- Manual log inspection on staging

### Risk 4: Parsing failures on synthetic data
**Mitigation**:
- Deterministic parser with safe failure handling
- Return null/empty for unknown fields (no guessing)
- Comprehensive unit tests covering edge cases
- Validation highlights missing fields for user correction

### Risk 5: Docker/deployment complexity
**Mitigation**:
- Docker compose for local dev parity
- Clear Dockerfile structure (multi-stage builds)
- Health checks on all services
- Environment variable documentation
- Deployment runbook in T20

---

## Further Considerations

### 1. Raw OCR Output Visibility (v0 Requirement)
**Question**: Should raw OCR `lines[]` be exposed in API response or only in a debug UI panel?

**Options**:
- **Option A**: Include `ocrRaw.lines[]` in API response (cleaner for v0 demo)
- **Option B**: UI debug panel only (keeps API contract cleaner)

**Recommendation**: Option A for v0, remove in v0.1 once parsing is functional

---

### 2. LLM Cleanup Integration Timing
**Question**: When should optional LLM parsing (`ENABLE_LLM_CLEANUP=false` flag) be implemented?

**Options**:
- After v1 completion as stretch goal
- Skip entirely for POC scope

**Recommendation**: Skip for POC unless parsing accuracy falls below 85% threshold after v1

---

### 3. Test Automation Coverage
**Question**: Should component tests for Web UI be included, or rely on manual testing?

**Options**:
- Include React component tests (react-testing-library)
- Manual testing only

**Recommendation**: Focus on backend tests (parser, validator); manual Web testing sufficient for POC

---

### 4. Cloud OCR Engine Testing
**Question**: Should evaluation harness (T19) run against cloud engines (Vision)?

**Options**:
- Include cloud engines in comparison (requires credentials + cost)

**Recommendation**: Local engines for initial evaluation; cloud engines optional if local accuracy is insufficient

---

### 5. Deployment Platform Flexibility
**Question**: Should deployment target other platforms besides Render/Vercel?

**Options**:
- Keep Render/Vercel as documented default
- Add alternative deployment guides (AWS, GCP, Azure)

**Recommendation**: Render/Vercel for POC; add alternatives if stakeholders require different platforms

---

## Success Criteria Checklist

- [ ] User can capture/upload UK license image via web UI
- [ ] Preview screen with retry works without page refresh
- [ ] API `/license/scan` returns structured fields + validation
- [ ] OCR worker protected with `X-INTERNAL-KEY`
- [ ] PaddleOCR engines functional
- [ ] Deterministic parser extracts 7 required fields
- [ ] Validation blocks expired licenses
- [ ] Validation enforces required fields
- [ ] UK postcode format validated in address line
- [ ] UK license number format validated
- [ ] Age warning (21-75) implemented
- [ ] Form auto-fills from API response
- [ ] Low confidence warning shows when <0.70
- [ ] Field-level blocking errors displayed
- [ ] Submit disabled when blocking errors exist
- [ ] User can manually edit all fields
- [ ] Optional fallback orchestration behind flag
- [ ] Synthetic dataset: 30 images (10/10/10)
- [ ] Ground truth JSON for all images
- [ ] Evaluation report generated
- [ ] ≥85% required-field accuracy achieved
- [ ] Median scan→form time ≤10s achieved
- [ ] No image persistence (verified)
- [ ] No PII in logs (verified)
- [ ] `Cache-Control: no-store` header present
- [ ] HTTPS on all staging services
- [ ] Security checklist completed
- [ ] End-to-end staging demo functional
- [ ] All SDLC gate artifacts complete

---

## Next Steps

1. **Validate plan with stakeholders**
   - Confirm milestone definitions
   - Confirm success metrics
   - Confirm deployment targets

2. **Set up development environment**
   - Install Docker + Docker Compose
   - Clone repository
   - Install dependencies (Java 17, Node.js, Python 3.11+)

3. **Begin v0 implementation**
   - Start with T1: Monorepo baseline
   - Proceed through tickets sequentially
   - Document any deviations or blockers

4. **Establish prompt logging practice**
   - Track AI prompts used for each ticket
   - Document what was kept vs changed
   - Maintain prompt log in `docs/gate4/02-prompt-log.md`

5. **Regular checkpoint reviews**
   - After v0 completion: review raw OCR output
   - After v0.1 completion: review parsing accuracy on sample images
   - After v1 completion: review full metrics vs success criteria
