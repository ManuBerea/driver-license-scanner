# Gate 4 — Prompt Log (AI-assisted, not AI-driven)

**Goal:** explicitly document how AI was used during delivery, ensuring decisions are reviewed and intentional.

This log is meant to be updated as work progresses.  
Each entry should correspond to a real usage of AI for implementation, debugging, design, or writing.

---

## Rules
- **No direct AI output committed without review**.
- Never paste secrets, keys, or real PII into prompts.
- Avoid sharing full license images or OCR text unless fully synthetic.
- Summarize outcomes and modifications so future readers can trust the work.

---

### Prompt-1 — T1: Create monorepo baseline
**Context**  
Needed a scalable monorepo layout that supports Web/API/OCR worker.

**What I asked**  
Create monorepo layout that supports Web/API/OCR worker.

**What AI suggested**  
- Monorepo with `web/`, `driver-license-scanner-api/`, `ocr-worker/`
- Clean src layouts per service
- .gitignore file

**What I kept**
- Monorepo structure
- .gitignore file

**What I changed and why**
- Ensured module boundaries align to Gate 4 (OCR / Parser / Validator)
- Ensured no using of nested git repositories (web subfolder was a nested git repository)

---

### Prompt-2 — Add docker compose and health endpoints
**Context**  
Finalizing tickets T1/T2 (monorepo baseline with health endpoints + docker compose)

**What I asked**  
Add docker compose configs and health endpoints to monorepo baseline.

**What AI suggested**  
- Dockerfiles per service 
- OCR worker endpoints
- Docker Compose configs
- Health actuator exposure in Spring Boot

**What I kept**  
- Service-specific Dockerfiles and Compose wiring
- FastAPI `/health` endpoint
- Actuator health exposure (YAML) for API health checks

**What I changed and why**  
- Removed `/ocr` endpoint to keep scope limited to T1/T2
- Migrated Spring config from `application.properties` to `application.yml`

---

### Prompt-3 — T3: Web capture/upload licence image
**Context**  
Build the capture/upload entry step with webcam + file upload, validation, and in-memory storage only. Multiple UX bugs were reported and iterated on.

**What I asked**  
[Prompt T3](../AI-context-docs/codex-prompts-used.md) (webcam + upload with type/size validation), fix camera start/stop issues, ensure no persistence, and improve UX text.

**What AI suggested**  
- Capture screen with webcam + upload  
- Client-side validation helper (type/size)  
- Clear error messages and disabled actions until ready  
- Keep image in memory only

**What I kept**  
- Everything

**What I changed and why**  
- Refactored into `components/`, `hooks`, `lib` to avoid a large `page.tsx` and improve maintainability  
- Fixed camera lifecycle issues (camera not stopping)
- Switched to a single Start/Stop toggle and removed confusing helper text
- Adjusted framing to UK license aspect ratio and improved size display (B/KB/MB)  
- Added drag-and-drop upload for easier use  

---

### Prompt-4 — T4: Web preview + retry before scan
**Context**  
Add a preview step after selection, with retry actions and a Scan button that only fires on click.

**What I asked**  
[Prompt T4](../AI-context-docs/codex-prompts-used.md) Implement preview with image display, Choose another / Retake, and Scan button (no auto upload), plus retry without refresh.

**What AI suggested**  
- Preview panel 
- Retry without page refresh
- Scan button
- Placeholder UI until API wiring  

**What I kept**  
- Everything

**What I changed and why**  
- Asked for potential bugs and fixes
- Fixed npm run lint errors and warnings 


### Prompt-5 - T5: API /license/scan stub + error handling
**Context**  
Implement the scan endpoint with multipart upload validation, stubbed response, and consistent error handling; update Postman for testing.

**What I asked**  
[Prompt T5](../AI-context-docs/codex-prompts-used.md) Implement POST /license/scan with validation, no-store responses, and standardized error payloads; also fix Postman tests and file upload issues.

**What AI suggested**  
- Create ScanController and DTOs for response contract
- Add global exception handling for 413/415 and multipart errors
- Enforce multipart size limits in Spring config
- Provide Postman collection updates and sample assets

**What I kept**  
- /license/scan endpoint with multipart validation and stubbed response
- Global exception handling for 413/415 and multipart errors
- Multipart size limits (10MB) in application.yml

**What I changed and why**  
- Simplified Postman collection to a single Scan request with manual file selection, because variable-based file paths were unreliable in Postman
- Added specific error codes/messages for missing image, too large, invalid format, and unsupported media type to make responses clearer

### Prompt-6 - T6: OCR Worker /health + /ocr stub
**Context**  
Expose an OCR worker surface so the API can call OCR without storing images.

**What I asked**  
[Prompt T6](../AI-context-docs/codex-prompts-used.md) Implement POST /ocr with multipart input, returning a stubbed response, no file persistence.

**What AI suggested**  
- POST /ocr accepting multipart image and returning stubbed response shape
- Keep everything in memory (no disk writes)

**What I kept**  
- Everything

**What I changed and why**  
- Updated Postman collection to include OCR /ocr request (health already existed)

---

### Prompt-7 - T7: Web scan wiring + results + retry
**Context**  
Wire the Scan button to the API and display results with a user-friendly retry path.

**What I asked**  
[Prompt T7](../AI-context-docs/codex-prompts-used.md) Connect Scan to POST /license/scan, show loading state, show errors without stack traces, and allow retry without refresh.

**What AI suggested**  
- Add a fetch call to /license/scan with multipart form data
- API wired to Scan button with loading and error handling
- Retry via same Scan button without refresh
- Extracted text fields along with engine used and confidence

**What I kept**  
- Add a fetch call to /license/scan with multipart form data
- API wired to Scan button with loading and error handling
- Retry via same Scan button without refresh

**What I changed and why**  
- Show only necessary extracted fields related to license (no engine/confidence) 
- Aligned preview layout to compare image vs extracted fields side-by-side
- Improved UI/UX styling (clearer panels, larger field blocks)
- Reorganized frontend to have css and js in separate folders

---

### Prompt-8 — OCR worker real OCR + engine abstraction (T8, T9)
**Context**  
Upgrade OCR worker from stub to real OCR with PaddleOCR, add engine abstraction.

**What I asked**  
Implement PaddleOCR + engine selection (paddle + cloud placeholders), protect `/ocr` with `X-INTERNAL-KEY`, keep images in memory, avoid PII logs, and add tests.

**What AI suggested**  
- Add `ocr_engines` with PaddleOCR implementation  
- Use `OCR_ENGINE` env to select engine
- Enforce `X-INTERNAL-KEY` auth  
- Return `lines[]`, confidence, timing  
- Add lightweight tests with a fake engine  
- Add Docker deps for PaddleOCR  
- Updated README to reflect real behavior

**What I kept**  
- Everything, but with the following improvements

**What I changed and why**  
- Set `OCR_ENGINE=paddle` in docker-compose to avoid unsupported `stub` engine  
- Added size and image validation with 400/413 and clearer errors  
- Refactored `main.py` into `core/` and `services/` for SRP

---

### Prompt-9 - T10: API - OCR worker integration
**Context**  
Wire the API to the OCR worker, handle failures cleanly, and resolve first-call timeouts.

**What I asked**  
- Implement OcrClient with timeouts + error mapping.  
- Fix `INVALID_IMAGE` multipart failures (worker received 0 bytes).  
- Make the first OCR call stable (avoid timeout during model download).

**What AI suggested**  
- RestTemplate client with `OCR_WORKER_URL` + `X_INTERNAL_KEY`.  
- Map timeout to `OCR_TIMEOUT` (504), other failures to `OCR_FAILED` (502).  
- Send multipart/form-data and add temporary logs to debug payload issues.

**What I kept**  
- OcrClient + internal auth header.  
- Timeout/error mapping.  
- Multipart/form-data approach (after fixing encoding).

**What I changed / directed during troubleshooting**  
- Verified the worker received **0 bytes**, then forced correct multipart encoding (file part + content type).  
- Added temporary logs to confirm the fix, then removed them for clean code.  
- Added warm-up + Docker cache volume to prevent first-call timeouts.

---

### Prompt-10 - T11+T12: Parser implementation (label-based extraction)
**Context**  
Improve field parsing accuracy from OCR lines while keeping logic deterministic and minimal.

**What I asked**  
Make parsing simpler and safer, remove postcode from the API contract and add it to the adress, and extract fields using labels with only minimal, high-confidence heuristics.

**What AI suggested**  
- Keep everything in one large parser file with many heuristics tied to specific OCR patterns.  

**What I kept**  
- The basic label-driven parsing approach, but asked for it to be refactored.  

**What I changed and why**  
- Split the huge parser into small parser files for each field and added helpers for SRP and readability.  
- Kept only generic, safe heuristics that improve accuracy (inline label handling, missing labels).  
- Removed `postcode` from the contract and folded it into `addressLine` (joined with “, ”), per requirements.  
- Kept dates in UK format `dd.MM.yyyy` as they are in the original licenses (no ISO conversion).  
- Added sanitization to drop special characters (keep letters/digits/comma/dot) for cleaner field output.  

---

### Prompt-11 â€” T13: Web editable driver form + low-confidence warning
**Context**  
Auto-fill an editable form from the API response and warn users on low-confidence scans.

**What I asked**  
Implement an editable form populated from the scan response, show a warning banner when `ocrConfidence` is below a configurable threshold (from API), highlight missing required fields, and ensure the user can review/correct all fields without refresh.

**What AI suggested**  
- Render a form after Scan with inputs for each field  
- Use `ocrConfidence` to conditionally show a warning (when < 5 fields are completed)
- Highlight missing required fields  
- Editable form bound to API response fields

**What I kept**  
- Everything, but with the following improvements 

**What I changed and why**  
- Made the warning threshold to come from API and be computed based on the number of fields. Initially the ocr confidence was added from the OCR engine, but it was too permissive..
- Made fields show their full text (auto-resizing inputs) to avoid truncation.  

