# Gate 3 — Test Plan (What is automated, what isn’t)

**Goal:** clearly define what will be automated vs manual for the POC.

---

## Automated tests (will implement)

### API (Spring Boot)
1. **Parser unit tests**
   - Given OCR lines/text, parse expected fields
   - Covers typical formats and common OCR errors
2. **Validator unit tests**
   - Expiry blocking
   - Licence number format validation
   - UK postcode format validation (within address line)
   - Required fields presence
   - Age rule (21–75) behavior
3. **OCR pipeline integration test (mocked)**
   - Scan endpoint tested end-to-end with mocked OCR response
   - Contract validation and error mapping

### OCR Worker (FastAPI)
1. `/health` endpoint test
2. `/ocr` contract-shape test (stub engine)
3. Optional smoke test for PaddleOCR output on one synthetic image (stretch)

### Web (Next.js)
- Lint + build checks (minimum)
- Optional small component test (stretch)

---

## Manual tests (will perform)

### UX checks
- Capture/upload on desktop
- Preview + retry
- Form auto-fill + manual override

### Performance checks
- Median time capture → form populated across dataset
- Confirm median ≤ 10 seconds target

### Privacy / guardrails checks
- Verify no OCR text or parsed fields appear in logs
- Verify no raw images stored on disk by default
- Verify API responses include `Cache-Control: no-store`
- Verify staging uses HTTPS

---

## Dataset-driven evaluation
Run the synthetic dataset buckets:
- 10 clean
- 10 medium
- 10 poor

Collect:
- Required-field accuracy (overall + per-field)
- Common failure patterns
- Median and p95 processing time
- % scans below confidence threshold

---

## Tools
- API tests: JUnit5
- OCR worker tests: pytest
- Web checks: ESLint + Next build
