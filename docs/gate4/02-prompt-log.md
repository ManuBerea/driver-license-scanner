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
[Prompt-T1 — Create monorepo baseline](../AI-context-docs/codex-prompts-used.md)

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

### Prompt-2 — Align T1/T2 tickets to baseline
**Context**  
Finalizing tickets T1/T2 (monorepo baseline with health endpoints + docker compose)

**What I asked**  
[Prompt T1 + T2](../AI-context-docs/codex-prompts-used.md)

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
