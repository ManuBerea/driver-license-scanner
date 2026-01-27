# Gate 4 — Clean Module Boundaries (OCR / Parser / Validator)

**Goal:** implement clean separation of responsibilities so the POC stays testable, maintainable, and safe.

---

## 1) Boundary rules (non-negotiable)

### OCR module
**Owns**
- image preprocessing (resize, grayscale, denoise, thresholding)
- OCR engine execution (PaddleOCR primary; optional others behind flags)
- raw text line extraction + confidences + timing

**Must NOT**
- parse business fields (name, DOB, address, etc.)
- apply business validation rules
- log OCR text or image contents

**Outputs**
- `OcrResult` (engineName, confidence, lines[], processingTimeMs, requestId)

---

### Parser module
**Owns**
- deterministic conversion of OCR output → structured fields:
  - firstName, lastName, dateOfBirth
  - addressLine, postcode
  - licenceNumber, expiryDate
  - categories (optional)

**Must NOT**
- call OCR engines directly (no external calls)
- validate business rules (expiry/age/required)
- guess values when uncertain

**Rules**
- prefer regex/heuristics
- normalize formats (dates, whitespace)
- return **null/empty** when unknown (no “best guess”)

**Outputs**
- `ParsedDriverFields` (structured fields only)

---

### Validator module
**Owns**
- backend authoritative validation rules:
  - expiry must not be in the past (blocking)
  - required fields must exist before save (blocking)
  - UK postcode format (blocking)
  - UK licence number format (blocking)
  - age between 21–75 (warning)

**Must NOT**
- run OCR
- parse OCR lines into fields
- mutate user input (only evaluate + report)

**Outputs**
- `ValidationResult`:
  - `blockingErrors[]` (field, code, message)
  - `warnings[]` (code, message)

---

## 2) Suggested code layout (maps to the boundaries)

### OCR Worker (FastAPI / Python)
```
ocr-worker/
  src/ocr_worker/
    api/routes.py                 # HTTP surface only
    services/ocr_service.py        # orchestration (engine selection)
    engines/
      base.py                      # OCREngine interface
      paddle_engine.py             # PaddleOCR implementation
    utils/image.py                 # preprocessing helpers
    core/config.py                 # env flags, thresholds
```

### API (Spring Boot / Java)
```
driver-license-scanner-api/
  controller/
    LicenseScanController.java
  service/
    ScanOrchestratorService.java      # calls OCR worker + parser + validator
  ocr/
    OcrClient.java                    # HTTP client to worker
    OcrResult.java
  parsing/
    UkLicenseParser.java
    ParsedDriverFields.java
  validation/
    UkLicenseValidator.java
    ValidationResult.java
  model/
    ScanResponse.java                 # contract returned to Web
```

## 3) Testing implications (why this boundary matters)

With clean boundaries:

✅ Parser unit tests can run without OCR  
✅ Validator unit tests can run without OCR  
✅ API integration test can mock `OcrResult` and still validate contract behavior  
✅ OCR worker can be tested in isolation for engine behavior

This directly supports Gate 5 deliverables (tests + accuracy report).

