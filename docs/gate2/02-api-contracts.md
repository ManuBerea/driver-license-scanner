# Gate 2 — API Contracts (Frontend ↔ API ↔ OCR Worker)

**Goal:** define stable request/response shapes so services can be developed independently.

---

## 1) Web → API: Scan license

### Endpoint
`POST /license/scan`

### Request
- `Content-Type: multipart/form-data`
- Form field:
  - `image` (file) — jpg/jpeg/png/webp

**Optional headers**
- `X-Request-Id` (client-generated UUID) — if provided, echoed back

### Successful response (200)
```json
{
  "requestId": "uuid",
  "selectedEngine": "paddle",
  "attemptedEngines": ["paddle"],
  "ocrConfidence": 0.82,
  "processingTimeMs": 920,
  "fields": {
    "firstName": "JOHN",
    "lastName": "SMITH",
    "dateOfBirth": "13.01.1990",
    "addressLine": "1 HIGH STREET, SW1A 1AA",
    "licenceNumber": "SMITH901133J99AB",
    "expiryDate": "13.01.2030",
    "categories": ["A", "AB"]
  },
  "validation": {
    "blockingErrors": [],
    "warnings": ["LOW_CONFIDENCE"]
  }
}
```

### Validation failure response (200)
Validation results are returned with the normal 200 response so the UI can render field-level errors:
```json
{
  "requestId": "uuid",
  "ocrConfidence": 0.75,
  "fields": { "...": "..." },
  "validation": {
    "blockingErrors": [
      {"code": "EXPIRED_LICENCE", "field": "expiryDate", "message": "Licence is expired"}
    ],
    "warnings": []
  }
}
```

### Error response (4xx/5xx)
```json
{
  "requestId": "uuid",
  "error": {
    "code": "OCR_FAILED",
    "message": "Scan failed. Please try again."
  }
}
```

**Standard error codes**
- `INVALID_IMAGE` — unsupported format or unreadable file
- `OCR_TIMEOUT` — scan exceeded timeout
- `OCR_FAILED` — OCR worker/engine failure
- `PARSING_FAILED` — OCR succeeded but no usable fields extracted

### Response headers
- `Cache-Control: no-store`

---

## 2) API → OCR Worker: Run OCR (internal)

### Endpoint
`POST /ocr`

### Request
- `Content-Type: multipart/form-data`
- Form field:
  - `image` (file)

**Internal auth header**
- `X-INTERNAL-KEY: <shared secret>` (recommended)

### Successful response (200)
```json
{
  "requestId": "uuid",
  "engine": "paddle",
  "confidence": 0.82,
  "rawText": "SMITH\nJOHN\n...",
  "lines": [
    {"text": "SMITH", "confidence": 0.93},
    {"text": "JOHN", "confidence": 0.91}
  ],
  "processingTimeMs": 620
}
```

### Error response (4xx/5xx)
```json
{
  "requestId": "uuid",
  "error": {
    "code": "OCR_FAILED",
    "message": "OCR engine failed"
  }
}
```

---

## 3) Contract compatibility notes

- The API can initially ignore `rawText` and rely on `lines[]`, or vice-versa.
- Confidence is interpreted as an overall score for the scan.
- Field-level confidence is optional and can be added later without breaking the contract.
- All error messages must be non-sensitive and exclude PII.
