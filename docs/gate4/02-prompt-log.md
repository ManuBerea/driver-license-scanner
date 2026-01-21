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
Finalizing tickets T1/T2 (monorepo baseline + compose bring-up) with stubbed worker health endpoints and DSL-friendly configs.

**What I asked**  
[Prompt T1 + T2](../AI-context-docs/codex-prompts-used.md)

**What AI suggested**  
- Clean `.gitignore` covering python/node/java artifacts
- Dockerfiles per service + minimal OCR worker endpoints
- Docker Compose wiring web/api/worker with ports and fake env defaults
- Health actuator exposure in Spring Boot

**What I kept**  
- Service-specific Dockerfiles and Compose wiring
- FastAPI `/health` stub + security guardrails
- Actuator health exposure (YAML) for API health checks

**What I changed and why**  
- Removed `/ocr` stub to keep scope limited to T1/T2
- Migrated Spring config from `application.properties` to `application.yml` as requested
