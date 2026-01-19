# Driver License Scanner

This repo contains a proof-of-concept for scanning a UK driver license image and returning extracted fields for form auto-fill.

## Projects

- `web/`  
  Next.js front-end for uploading/capturing license images and auto-filling a form.

- `driver-license-scanner-api/`  
  Spring Boot API that orchestrates the pipeline: receives images, calls OCR worker, parses fields, applies validation, and returns structured data.

- `ocr-worker/`  
  FastAPI service that performs OCR and returns raw text + confidence.
