# OCR Worker (FastAPI)

Internal OCR service for the Driver License Scanner POC. This service runs OCR in memory only and never stores images to disk.

## Guardrails

- No image persistence.
- No OCR text or parsed fields are logged.
- Internal access only via `X-INTERNAL-KEY`.

## Environment Variables

- `X_INTERNAL_KEY` (required)
- `OCR_ENGINE` = `paddle|doctr|vision|textract` (default: `paddle`)
- `ENABLE_VISION_OCR` = `false` (default)
- `ENABLE_TEXTRACT_OCR` = `false` (default)
- `ENABLE_OCR_RAW_TEXT` = `false` (default)
- `MAX_IMAGE_BYTES` = `10485760` (default: 10MB)

## Python Version

PaddleOCR/PaddlePaddle wheels are most reliable on Python 3.10 for Windows and Docker builds. Use Python 3.10 for the OCR worker.

## Dependencies

Default install is Paddle-only. Optional docTR and dev/test dependencies are split out.

```bash
# Paddle-only runtime
pip install -r requirements.txt

# Optional docTR support
pip install -r requirements-doctr.txt

# Dev/test tools
pip install -r requirements-dev.txt
```

## Local Run

```bash
pip install -r requirements.txt
uvicorn main:application --host 0.0.0.0 --port 8000
```

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

## Docker (optional docTR)

To build an image with docTR support:

```bash
docker build --build-arg ENABLE_DOCTR=true -t ocr-worker:doctr .
```
