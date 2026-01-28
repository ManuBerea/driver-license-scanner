# OCR Worker (FastAPI)

Internal OCR service for the Driver License Scanner POC. This service runs OCR in memory only and never stores images to disk.

## Guardrails

- No image persistence.
- No OCR text or parsed fields are logged.
- Internal access only via `X-INTERNAL-KEY`.

## Environment Variables

- `X_INTERNAL_KEY` (required)
- `OCR_ENGINE` = `paddle|vision` (default: `paddle`)
- `ENABLE_VISION_OCR` = `false` (default)
- `ENABLE_OCR_RAW_TEXT` = `false` (default)
- `MAX_IMAGE_BYTES` = `10485760` (default: 10MB)
- `GOOGLE_APPLICATION_CREDENTIALS` (required for Vision OCR; path to service account JSON)

## Python Version

PaddleOCR/PaddlePaddle wheels are most reliable on Python 3.10 for Windows and Docker builds. Use Python 3.10 for the OCR worker.

## Dependencies

Default install is Paddle-only. Dev/test dependencies are split out.

```bash
# Paddle-only runtime
pip install -r requirements.txt

# Dev/test tools
pip install -r requirements-dev.txt
```

## Local Run

```bash
pip install -r requirements.txt
uvicorn main:application --host 0.0.0.0 --port 8000
```

## Vision OCR Setup (optional)

1) Enable Cloud Vision API in your Google Cloud project.  
2) Create a service account key JSON and set:

```bash

export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
export ENABLE_VISION_OCR=true
```

When `ENABLE_VISION_OCR=true`, the API can call the worker with `X-OCR-ENGINE: vision`.

## Tests

```bash
pytest
```

## API

- `GET /health` -> `{ "status": "ok" }`
- `POST /ocr` -> OCR response

Example request:

```bash
curl -X POST http://localhost:8000/ocr \
  -H "X-INTERNAL-KEY: dev-internal-key" \
  -F "image=@path/to/synthetic-license.png"
```
