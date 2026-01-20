# Gate 0 — Final Success Metrics

These metrics define whether the POC is considered successful.

---

## 1) Field Accuracy (Primary)
**Minimum requirement**
- **≥ 85% field accuracy** on the synthetic test dataset.

**Interpretation**
- Accuracy is measured per required field:
  - First name
  - Last name
  - Date of birth
  - Address line + postcode
  - License number
  - Expiry date

**Pass condition**
- Across all dataset samples, the percentage of correct extracted field values is ≥85%.

---

## 2) Speed / Latency (User Experience)
**Minimum requirement**
- Median time from capture to populated form **≤ 10 seconds**.

**Pass condition**
- Record timing from “Scan” request start → API response returned → form populated.
- Median across dataset runs ≤ 10 seconds.

---

## 3) Low Confidence Handling (User Safety)
**Minimum requirement**
- OCR confidence **< 70% always triggers a warning**.

**Pass condition**
- UI shows a consistent warning banner: “Low confidence — please review fields”
- Threshold configurable via environment variable

---

## 4) Expiry Validation (Business Rule Enforcement)
**Minimum requirement**
- License expiry validation **blocks submission**.

**Pass condition**
- If expiry date is in the past, the form cannot be submitted and a clear message is shown.

---

## 5) Clear Error Messaging (Reliability)
**Minimum requirement**
- Clear user messaging for **all failure cases** (no crashes / no stack traces).

**Pass condition**
- Errors return non-sensitive error codes (e.g., OCR_FAILED, OCR_TIMEOUT)
- User can retry without refreshing the page

---

## 6) Privacy Guardrail Compliance (Pass/Fail)
**Hard constraints (must all be true)**
- No real personal data used for testing (synthetic/anonymized only)
- No raw license images stored by default
- No PII in logs/analytics/error reporting
- HTTPS only
- All AI usage documented (prompt log, decisions)

**Pass condition**
- Any violation fails the POC regardless of accuracy/speed results.

---

## Stretch Metrics (Optional / Nice-to-have)
- Field-by-field accuracy report
- Confidence-based UI indicators
- Performance comparison between OCR approaches (Paddle vs docTR vs Vision vs Textract)
