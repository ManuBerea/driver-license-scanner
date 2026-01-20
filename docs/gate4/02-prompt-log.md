# Gate 4 — Prompt Log (AI-assisted, not AI-driven)

**Goal:** explicitly document how AI was used during delivery, ensuring decisions are reviewed and intentional.

This log is meant to be updated as work progresses.  
Each entry should correspond to a real usage of AI for implementation, debugging, design, or writing.

---

## Rules
- **No direct AI output committed without review**.
- Never paste secrets, keys, or real PII into prompts.
- Avoid sharing full licence images or OCR text unless fully synthetic.
- Summarize outcomes and modifications so future readers can trust the work.

---

### Prompt-T1 — Create monorepo baseline
**Context**  
Needed a scalable monorepo layout that supports Web/API/OCR worker.

**What I asked**  
[Prompt-T1 — Create monorepo baseline](03-prompts-used.md)

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

## Template for future entries (copy/paste)

### Prompt-T1 — <short title>
**Context**  

**What I asked**  

**What AI suggested**  

**What I kept**  

**What I changed and why**
