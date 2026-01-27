# Gate 2 — Key Technical Decisions & Tradeoffs

**Goal:** document important choices and why they were made so the POC stays aligned and defensible.

---

## Decision 01 — Hybrid backend (Spring Boot + FastAPI)
**Choice**
- Spring Boot API for orchestration/validation
- FastAPI worker for OCR execution

**Why**
- Spring Boot fits the broader backend ecosystem and validation layer
- Python has best OCR tooling (OpenCV + PaddleOCR)
- Separation keeps the OCR runtime isolated and replaceable

**Tradeoffs**
- Two services increases operational complexity
- Requires contract discipline and Docker compose for local dev

---

## Decision 02 — Deterministic parsing first (LLM behind flag)
**Choice**
- Default parser uses regex + heuristics
- Optional LLM cleanup/parsing behind `ENABLE_LLM_CLEANUP=false`

**Why**
- Deterministic = testable, debuggable, and avoids hallucinations
- LLM can help in edge cases but must not be required to pass basic POC

**Tradeoffs**
- Deterministic parsing may miss some fields on poor OCR
- LLM integration adds privacy considerations and documentation requirements

---

## Decision 03 — Confidence threshold = 0.70
**Choice**
- `OCR_CONFIDENCE_WARN_THRESHOLD=0.70`

**Why**
- Matches explicit requirement: warn when confidence < 70%
- Provides a clear UX signal to review results

**Tradeoffs**
- Confidence calibration varies between engines
- May warn more than necessary initially (acceptable for safety)

---

## Decision 04 — Fallback OCR only behind flag
**Choice**
- `ENABLE_FALLBACK_OCR=false` by default
- When enabled, fallback order: Paddle → Vision

**Why**
- Keeps POC simple by default and reduces costs/keys dependency
- Allows future benchmarking and improvements

**Tradeoffs**
- Without fallback, some poor-quality images may fail
- With fallback enabled, latency can increase

---

## Decision 05 — No persistence / no PII logs (hard constraints)
**Choice**
- No raw images stored
- No OCR text or fields logged
- Cache disabled

**Why**
- Non-negotiable guardrails in the POC brief

**Tradeoffs**
- Harder debugging without samples in logs
- Requires synthetic dataset + reproducible tests instead of log inspection

---

## Decision 06 — Deployment targets
**Choice**
- Web: Vercel
- API: Render (Docker)
- OCR Worker: Render (Docker)

**Why**
- Simple deployment path for a POC
- Supports separation and environment variables

**Tradeoffs**
- OCR worker might need extra CPU/RAM
- Private networking may be limited depending on the hosting plan. Solution: the calls will be protected using an internal header
