# Gate 3 — Milestones (v0 → v0.1 → v1)

## v0 — Capture + raw OCR output
**Objective**  
Ship a working end-to-end flow where a user can upload/capture an image and receive **raw OCR output** (or stub) without crashes.

**Includes**
- Web: capture/upload + preview + scan button
- API: `POST /license/scan` accepts image and returns stub/early response
- OCR worker: `/ocr` stub or minimal output

**Exit criteria**
- Web → API → OCR worker integration works
- Clear errors + retry path
- No crashes, no PII logs, no persistence

---

## v0.1 — Parsed fields + form autofill
**Objective**  
Return **structured driver fields** and auto-fill the form.

**Includes**
- PaddleOCR integrated in OCR worker (real OCR)
- API deterministic parsing
- Web auto-fill editable form
- Low confidence warning (<0.70)

**Exit criteria**
- On clean/medium synthetic images, the form is populated with usable values
- Fields are editable and required fields are highlighted

---

## v1 — Validation + tests + deploy
**Objective**  
Make the POC “mini product” ready: reliable, measurable, and deployed.

**Includes**
- Backend validation rules enforced
- Synthetic dataset (30 images: 10 clean/10 medium/10 poor)
- Automated tests (parser, validator, mocked pipeline integration)
- Accuracy report (per-field + failure patterns)
- Staging deployment (HTTPS)

**Exit criteria**
- Meets success metrics: ≥85% accuracy, ≤10s median
- Guardrails satisfied (no PII logs, no persistence)
- Live staging demo and docs ready
