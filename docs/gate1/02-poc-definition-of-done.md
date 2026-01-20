# Gate 1 — POC Definition of Done

This POC is considered **complete** when the system meets **all** functional requirements, success metrics, and guardrails,
and when the end-to-end flow is demonstrably testable and repeatable on the synthetic dataset.

---

## 1) End-to-end user flow works
A motor trade staff member can:
1. Capture or upload a UK driving licence image
2. Preview and retry prior to scanning
3. Submit image to the backend
4. Receive OCR + parsed fields
5. See an auto-filled editable driver form
6. Correct fields manually and attempt to submit

---

## 2) Required fields are extracted (when possible)
The system extracts and returns these required fields when present and readable:
- First name
- Last name
- Date of birth
- Address (line + postcode)
- Licence number
- Expiry date

Optional/stretch:
- Licence categories

If a field cannot be found, the system returns **null/empty**, not a guess.

---

## 3) Validation rules enforced (authoritative in backend)
The backend enforces:
- Licence expiry **blocks submission**
- Required fields must be present before save
- UK postcode format validation
- UK licence number format validation
- Age between 21–75 validation

Validation errors are shown clearly and associated to the relevant fields.

---

## 4) Low confidence behavior is consistent
- When `ocrConfidence < 0.70`, the UI shows:  
  **“Low confidence — please review fields”**
- The threshold is configurable via environment variable.

---

## 5) Success metrics met
The POC meets the baseline success criteria on the synthetic dataset:
- **≥ 85% field accuracy** on required fields
- **Median time** from capture to populated form **≤ 10 seconds**
- Expiry validation blocks submission
- Clear error messaging for failure cases

---

## 6) Clear and safe error handling exists
All failures return:
- A non-sensitive error code (e.g., `OCR_FAILED`, `OCR_TIMEOUT`)
- A human-readable message
- A retry path without refresh

No stack traces or PII appear in UI messages.

---

## 7) Privacy and security guardrails are satisfied (pass/fail)
These are mandatory:
- Only synthetic/anonymized data used for testing
- No raw licence images stored by default
- No OCR text / parsed PII logged
- HTTPS only (staging)
- AI usage documented

Any guardrail violation = **POC fails**, regardless of metrics.

---

## 8) Testability
Behavior is testable via:
- A synthetic dataset (≥ 30 images) used consistently for evaluation
- Automated tests can be written against deterministic outputs
- Metrics are captured (processing time, success/failure, confidence)
