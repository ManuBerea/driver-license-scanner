# AI POC Brief
Driver License ID Scan & Auto-Fill

### Purpose
This POC exists to train a developer to work with AI across the full SDLC, not just to build a demo.
Success is measured by quality, structure, and learning artifacts, not lines of code or UI polish.

This POC should behave like a mini product: scoped, validated, shipped, and reviewed.

## 1. Problem Statement
Manual entry of driver license data is slow, error-prone, and frustrating in motor trade workflows.
This POC explores whether OCR + AI-assisted parsing can reliably extract and auto-fill driver data from UK driving licenses, while remaining privacy-safe and operationally realistic.

## 2. Target User
Internal motor trade staff or brokers
Goal: create or validate a driver profile quickly and accurately
Context: browser or mobile, non-technical user

## 3. Scope
In scope
Capture a UK driving license via webcam or image upload
OCR extraction and structured parsing
Auto-fill editable driver form
Business validation (age, expiry, format)
Deployment to a public staging environment
Test dataset, metrics, and documentation
Out of scope
Real backend persistence
Real DVLA integration
Authentication/authorization
Supporting non-UK IDs

## 4. Non-Negotiable Guardrails
These are hard constraints.
No real personal data used for testing
Only synthetic or anonymized images
No raw license images stored by default
No PII in logs, analytics, or error reporting
HTTPS only
All AI usage must be documented (prompts, decisions)

If any of these are violated, the POC is considered failed.

## 5. Success Metrics (Definition of Success)
Minimum acceptable outcomes:
≥ 85% field accuracy on synthetic test dataset
Median time from capture to populated form ≤ 10 seconds
OCR confidence < 70% always triggers a warning
License expiry validation blocks submission
Clear error messaging for all failure cases
Stretch (optional):
Field-by-field accuracy report
Confidence-based UI indicators
Performance comparison between OCR approaches

## 6. Functional Requirements
Fields to extract
First name
Last name
Date of birth
Address line (includes postcode)
License number
Expiry date
License categories (optional)

Validation rules
UK license number format
Age between 21–75 (warning or error as defined)
License must not be expired
Valid UK postcode present in address line
Required fields must be present before save

## 7. SDLC Gated Delivery Plan

You must complete each gate with artifacts before moving forward.

### Gate 0 – Discovery & Framing
Goal: understand the problem and define “done”.
### Deliverables:
1-page problem framing
Assumptions and risks list (top 5)
Final success metrics
Updated scope confirmation

### Gate 1 – Requirements & Acceptance
Goal: make behavior testable.
### Deliverables:
15 user stories max
Acceptance criteria per story
POC Definition of Done
### Example:
Given a valid UK driving license image, when OCR confidence ≥ 70%, then at least 5 required fields are auto-filled correctly.

### Gate 2 – Architecture & Design
Goal: force tradeoff thinking.
### Deliverables:
Architecture diagram 
Data flow diagram highlighting PII
Decision log:
client-side OCR vs server-side
fallback strategy (Paddle -> Google Vision)

### Gate 3 – Planning
Goal: intentional execution.
### Deliverables:
Backlog (10–20 tickets)
### Milestones:
v0: capture + raw OCR output
v0.1: parsed fields + form autofill
v1: validation + tests + deploy
Test plan (what is automated, what isn’t)

Gate 4 – Implementation (AI-assisted, not AI-driven)
Goal: learn how to work with AI.
### Deliverables:
Clean module boundaries (ocr, parser, validator)
Prompt log:
what you asked
what you kept
what you changed and why
No direct AI output committed without review

### Gate 5 – Quality Engineering
Goal: reliability over demos.
### Deliverables:
Synthetic test dataset (min 30 images)
10 clean
10 medium quality
10 poor quality
Automated tests:
parser unit tests
validator unit tests
OCR pipeline integration test (mocked)
Accuracy report:
per-field accuracy
common failure patterns

This is mandatory. Skipping tests = failing the POC.

### Gate 6 – Security & Privacy
Goal: production mindset.
### Deliverables:
PII checklist:
where data flows
how long it exists
what is logged
Security basics:
no caching of images
secure headers
environment separation

### Gate 7 – Release & Ops
Goal: ship and support.
### Deliverables:
Deployed staging environment (Example: Render)
Basic observability:
OCR success/failure
processing time
### Runbook:
how to deploy
what breaks
how to rollback
Demo script

### Gate 8 – Retro & Learning Extraction
Goal: turn effort into reusable knowledge.
### Deliverables:
Retro doc:
where AI helped
where AI misled
top 5 engineering lessons
Recommendations for next AI POC

## 8. Tech Stack
### Frontend
Next.js 14 + TypeScript
React Hook Form + Zod + date-fns
TailwindCSS
### Backend (hybrid)
Spring Boot 3 (API + orchestration + validation + metrics)
Python FastAPI OCR worker (OpenCV + PaddleOCR)
Feature flags via env vars
### AI Stack
OCR: PaddleOCR (primary)
Preprocessing: OpenCV (+ Albumentations optional)
Parsing: deterministic parser (regex/heuristics)
Validation: backend authoritative rules
Optional LLM parsing/cleanup behind flags (Gemini/OpenAI)
Optional fallback OCR: Google Vision behind flags
### Infra / Deploy
Docker + Render (Spring Boot API + FastAPI OCR worker)
Vercel (Next.js)
HTTPS enforced
Prometheus/Micrometer metrics
Sentry (sanitized)
### Testing
JUnit5 (Java)
pytest (Python)
Synthetic dataset in repo (only fake)

## 9. Expected Outcome
At the end, we should have:
A working POC
Understanding: how  AI can help in delivery
