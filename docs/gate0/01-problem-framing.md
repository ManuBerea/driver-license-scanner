# Gate 0 — Problem Framing (1 page)

## Title
UK Driving License Scan & Auto-Fill POC

## Why this POC exists
This POC exists to **train a developer to work with AI across the full SDLC**, not to build a flashy demo. Success is measured by the **quality, structure, and learning artifacts** produced through gated delivery (discovery → requirements → architecture → implementation → quality → security → release → retro).

The POC should behave like a **mini product**: scoped, validated, shipped, and reviewed.

## The problem
Manual entry of driver license data is **slow, error-prone, and frustrating** in motor trade workflows.
The goal is to determine whether **OCR + AI-assisted parsing** can reliably extract key driver details from **UK driving licenses** and auto-fill a driver form—while remaining **privacy-safe** and **operationally realistic**.

## Target user
**Internal motor trade staff or brokers** who need to create or validate a driver profile quickly and accurately. The user is non-technical and operates in a browser/mobile environment.

## What we are building (high-level)
A web-based workflow:

1. **Capture / Upload** a UK driving license image (webcam or file upload)
2. **Preview** and optionally retry for better scan quality
3. **Scan**: submit image to backend
4. **OCR Extraction**: extract text from the image
5. **Parsing**: convert OCR output into structured driver fields
6. **Auto-fill Form**: populate an editable driver form
7. **Validate** business rules (expiry, required fields, formats)
8. **User Feedback**: warnings/errors + retry paths

## Field extraction requirements
Required fields:
- First name
- Last name
- Date of birth
- Address (line + postcode)
- License number
- Expiry date

Optional:
- License categories (optional/stretch)

## Validation requirements
- UK license number format validation
- Age rule: 21–75 
- License must not be expired (blocks submission)
- Valid UK postcode format
- Required fields must exist before save

## Non-negotiable guardrails (hard constraints)
If any of these are violated, the POC is considered failed:

- No real personal data used for testing (synthetic/anonymized only)
- No raw license images stored by default
- No PII in logs, analytics, or error reporting
- HTTPS only
- All AI usage must be documented (prompts, decisions)

## Proposed technical approach (initial framing)
Frontend:
- Next.js 14 + TypeScript
- React Hook Form + Zod + date-fns
- Tailwind CSS

Backend (hybrid):
- Spring Boot 3: API orchestration + validation + metrics
- FastAPI OCR worker: OpenCV + PaddleOCR/docTR
- Feature flags via env vars

OCR/AI stack:
- OCR primary: PaddleOCR
- Optional benchmark: docTR
- Optional fallback behind flags: Google Vision OCR and AWS Textract
- Parsing: deterministic parser first (regex/heuristics)
- Optional LLM cleanup/parsing behind flags (Gemini/OpenAI)

## Definition of “Done” (Gate 0 aligned)
We are “done” when we can demonstrate a privacy-safe end-to-end scan workflow that:
- Autofills key driver fields reliably
- Enforces validation rules correctly
- Meets agreed success metrics on a synthetic dataset
- Produces the required SDLC learning artifacts and documentation
