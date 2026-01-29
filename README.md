# Driver License Scanner

This repo contains a proof-of-concept for scanning a UK driver license image and returning extracted fields for form auto-fill.

## Projects

- `web/`  
  Next.js front-end for uploading/capturing license images and auto-filling a form.

- `driver-license-scanner-api/`  
  Spring Boot API that orchestrates the pipeline: receives images, calls OCR worker, parses fields, applies validation, and returns structured data.

- `ocr-worker/`  
  FastAPI service that performs OCR and returns raw text + confidence.

---

## Quick start (project setup)

### 1) Use Docker Compose (recommended)

1. Copy env template:
   ```
   copy .env.example .env
   ```
2. Edit `.env` (only needed if you want Vision OCR):
   - Set `ENABLE_VISION_OCR=true`
   - Set `GOOGLE_APPLICATION_CREDENTIALS_PATH=C:\path\to\service-account.json`

Here you need to provide a path to your Google Cloud service account JSON file with Vision API access.
3. Start the stack (all services - you need to be at repo root):
   ```
   docker compose up -d --build
   ```

Open:
- Web: http://localhost:3000
- API: http://localhost:8080

### 2) Run API locally (no Docker)

Set env once (Windows PowerShell):
```
setx OCR_WORKER_URL "http://localhost:8000"
setx X_INTERNAL_KEY "dev-internal-key"
setx SPRING_PROFILES_ACTIVE "local"
```

macOS equivalents:

```
export OCR_WORKER_URL=http://localhost:8000
export X_INTERNAL_KEY=dev-internal-key
export SPRING_PROFILES_ACTIVE=local
```

Then reopen the cmd/terminal and run:
```
cd driver-license-scanner-api
.\gradlew clean build bootRun
```

macOS equivalents:
```
cd driver-license-scanner-api
./gradlew clean build bootRun
```

### 3) Run web locally (no Docker)

```
cd web
npm install
npm run dev
```

macOS equivalents:

```
cd web
npm install
npm run dev
```

### 4) Run OCR worker locally (no Docker)

```
cd ocr-worker
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:X_INTERNAL_KEY="dev-internal-key"
python main.py
```

macOS equivalents:

```
cd ocr-worker
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export X_INTERNAL_KEY=dev-internal-key
python main.py
```

### Evaluation report

Run the evaluation runner (generates `driver-license-scanner-api/reports/ocr_engine_comparison.md`):
```
cd driver-license-scanner-api
.\gradlew runEvaluation
```

Compare API outputs vs ground truth (you need to be at repo root):
```
python scripts/compare_scan_outputs.py --api-url http://localhost:8080/license/scan --dataset docs/images --out reports/scan_output_diff.json
```
