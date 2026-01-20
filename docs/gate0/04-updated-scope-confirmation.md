# Gate 0 — Updated Scope Confirmation

This document confirms the agreed POC scope, boundaries, and what “done” means at a product level.

---

## 1) In Scope (Committed)
The POC **will include**:

### A) Capture & UX flow
- Capture a **UK driving license** via webcam OR upload image file
- Image preview + retry before scanning
- Supported formats: jpg, jpeg, png, webp
- Max file size limit enforced (e.g., 5–10MB)

### B) OCR extraction & parsing
- Server-side OCR execution (primary: PaddleOCR)
- OCR output returned as raw lines + confidence
- Structured parsing into driver fields (deterministic parser first)

### C) Auto-fill editable form
- Auto-fill an editable driver form
- Missing fields highlighted as required
- User can override auto-filled values

### D) Business validation rules
- License expiry blocks submission
- Required fields enforced
- UK postcode format validation
- UK license number format validation
- Age rule (21–75) implemented consistently (warning)

### E) Deployment & documentation
- Deployment to a **public staging environment**
- Test dataset + metrics
- Documentation and SDLC artifacts per gate plan

---

## 2) Out of Scope (Explicitly Excluded)
The POC **will not include**:

- Real backend persistence / database storage
- DVLA integration
- Authentication / authorization
- Support for non-UK IDs

---

## 3) Non-Negotiable Guardrails (Pass/Fail)
These constraints are mandatory; violating them fails the POC:

- No real personal data used for testing
- Only synthetic or anonymized images
- No raw license images stored by default
- No PII in logs, analytics, or error reporting
- HTTPS only
- All AI usage must be documented (prompts, decisions)

---

## 4) Tech Stack Confirmation (POC baseline)
Frontend:
- Next.js 14 + TypeScript
- React Hook Form + Zod + date-fns
- Tailwind CSS

Backend:
- Spring Boot 3 (API orchestration + validation + metrics)
- FastAPI OCR worker (OpenCV + PaddleOCR/docTR)
- Feature flags via environment variables

OCR / AI:
- OCR primary: PaddleOCR
- Optional benchmark: docTR
- Optional fallback engines behind flags: Google Vision OCR / AWS Textract
- Parsing: deterministic parser (regex/heuristics) first
- Optional LLM cleanup behind flags (Gemini/OpenAI)

---

## 5) Definition of Done (Scope-level)
This POC is considered complete when:

1. A user can capture/upload a UK driving license image and preview it
2. Scanning returns structured driver fields and populates the form
3. Validation rules behave correctly (expiry blocks, required fields enforced)
4. Low-confidence (<70%) consistently triggers a UI warning
5. Median scan-to-form time ≤ 10 seconds
6. ≥85% field accuracy is achieved on synthetic dataset
7. POC is deployed to staging (HTTPS)
8. Required SDLC gate artifacts are present and complete
