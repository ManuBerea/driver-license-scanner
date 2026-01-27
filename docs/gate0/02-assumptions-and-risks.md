# Gate 0 — Assumptions & Risks (Top 5)

This list reflects the most important assumptions we are making early, plus the highest-impact risks if they are wrong.

---

## 1) Assumption: OCR accuracy is achievable on UK license photos
**Assumption**
We can reach the required level of accuracy (≥85% field accuracy on the synthetic dataset) using OCR + parsing on typical photo captures (not perfect scans).

**Risk**
Mobile photos may have glare, blur, motion, poor exposure, or partial cropping; OCR might struggle, especially on “poor quality” samples.

**Mitigation**
- Include quality buckets (10 clean / 10 medium / 10 poor) in dataset and measure accuracy separately
- Provide UI retry (“Retake/Choose another”) before scanning
- Add preprocessing (OpenCV) early (contrast/deskew/crop assistance)
- Add fallback OCR engines behind flags (Vision)

---

## 2) Assumption: Deterministic parsing is sufficient for the required fields
**Assumption**
A rules-based parser (regex/heuristics) can reliably extract the required fields from OCR text lines without needing an LLM in the default path.

**Risk**
OCR output may be inconsistent (broken lines, misread characters), causing parsing failures or wrong assignments (e.g., name vs address lines).

**Mitigation**
- Parser must fail safely (nulls vs hallucinations)
- Normalize whitespace/dates/casing
- Add field-level validation as a second safety net
- Only enable LLM cleanup behind a feature flag and log prompts/decisions

---

## 3) Assumption: Latency remains acceptable within 10 seconds median
**Assumption**
Median time from capture to populated form can stay ≤10 seconds even with OCR and parsing.

**Risk**
OCR may be slow, especially with large images or fallback chaining across engines. This can degrade user trust and usability.

**Mitigation**
- Enforce file size limits (e.g., 5–10MB) in UI
- Provide loading state and clear timeout errors with retry option
- Capture processing-time metrics in backend
- Keep fallback engines OFF by default and triggered only when needed

---

## 4) Assumption: Privacy-safe processing is feasible end-to-end
**Assumption**
We can deliver the full workflow without persisting license images and without logging PII.

**Risk**
Accidental logging of OCR text / extracted fields is easy during debugging. Caching or analytics could leak PII.

**Mitigation**
- Explicit logging rules: never log images/OCR text/fields
- Add `Cache-Control: no-store` responses
- Keep OCR worker private / protected with internal header (e.g., X-INTERNAL-KEY)
- Maintain a security & PII checklist and treat guardrails as pass/fail

---

## 5) Assumption: The scope is contained and does not expand into real production integration
**Assumption**
We will not add real persistence, DVLA integration, authentication/authorization, or non-UK IDs.

**Risk**
Scope creep can derail POC learning goals and delay delivery of required artifacts and quality gates.

**Mitigation**
- Publish a scope confirmation doc (this gate)
- Enforce the SDLC gates and definition of done
