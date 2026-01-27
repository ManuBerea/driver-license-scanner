# Gate 2 — Data Flow & PII Boundaries

**Goal:** explicitly document where sensitive data flows and how we prevent leakage.

---

## Data classification

**PII / Sensitive**
- Raw driver licence image
- Extracted OCR text
- Parsed driver fields (name, DOB, address, licence number)

**Non-sensitive metadata**
- requestId
- timing metrics (ms)
- engine name
- confidence score
- error codes

---

## Data flow diagram highlighting PII
```mermaid
flowchart TB
 subgraph Guardrails["Controls (must hold everywhere)"]
    direction LR
        G1["No image persistence<br>(no disk, no DB)"]
        G2["No PII logs<br>(no OCR text, no fields in logs)"]
        G3["Cache-Control: no-store<br>(scan responses)"]
        G4["Cloud OCR opt-in only<br>(explicit enable + creds)"]
        G5["HTTPS in staging"]
  end
 subgraph WebZone["Web (Client)"]
    direction TB
        Capture["Capture/Upload/Paste<br>PII: RAW IMAGE"]
        Preview["Preview + Retry<br>(in-memory only)"]
        Send["POST /license/scan<br>multipart image"]
        Form["Editable Driver Form<br>PII: STRUCTURED FIELDS"]
        Save["Save/Continue<br>(no storage in POC)"]
  end
 subgraph ApiZone["API Orchestrator (Spring Boot)"]
    direction TB
        ApiIngress["/license/scan<br>Validates file<br>Cache-Control: no-store"]
        ApiMem["In-memory image bytes<br>PII: RAW IMAGE"]
        OcrCall["Call OCR Worker<br>POST /ocr<br>X-INTERNAL-KEY"]
        Parse["Parser Module<br>PII: OCR TEXT → FIELDS"]
        Validate["Validator Module<br>PII: FIELDS"]
        ApiResp["Response Contract<br>fields + validation + confidence<br>PII: STRUCTURED FIELDS"]
  end
 subgraph WorkerZone["OCR Worker (FastAPI)"]
    direction TB
        WorkerIngress["/ocr receives image<br>in-memory only<br>PII: RAW IMAGE"]
        Preprocess["Optional preprocessing<br>(OpenCV)"]
        EngineSelect["OCR Engine Selection"]
        Paddle["PaddleOCR (default)"]
        Vision["Google Vision (cloud)"]
        OcrOut["OCR Output<br>PII: OCR TEXT lines[] + confidence"]
  end
 subgraph Legend["PII Data Classes"]
    direction LR
        L1["RAW IMAGE (PII source)<br>license photo, face/address visible"]
        L2["OCR TEXT (PII)<br>raw extracted text lines"]
        L3["STRUCTURED FIELDS (PII)<br>Name, DOB, address, license no., expiry"]
  end
    Capture --> Preview
    Preview --> Send
    Send --> ApiIngress
    ApiIngress --> ApiMem
    ApiMem --> OcrCall
    OcrCall --> WorkerIngress
    WorkerIngress --> Preprocess
    Preprocess --> EngineSelect
    EngineSelect --> Paddle
    EngineSelect -. "opt-in" .-> Vision
    Paddle --> OcrOut
    Vision -.-> OcrOut
    OcrOut --> Parse
    Parse --> Validate
    Validate --> ApiResp
    ApiResp --> Form
    Form --> Save
    Save --> End["Complete"]
    Guardrails ~~~ WebZone & ApiZone & WorkerZone
    User["User"] --> WebZone

     G1:::control
     G2:::control
     G3:::control
     G4:::control
     G5:::control
     Capture:::piiImage
     Preview:::normal
     Send:::normal
     Form:::piiFields
     Save:::normal
     ApiIngress:::normal
     ApiMem:::piiImage
     OcrCall:::normal
     Parse:::piiText
     Validate:::piiFields
     ApiResp:::piiFields
     WorkerIngress:::piiImage
     Preprocess:::normal
     EngineSelect:::normal
     Paddle:::normal
     Vision:::normal
     OcrOut:::piiText
     WebZone:::normal
    classDef piiImage fill:#FFCDD2,stroke:#C62828,stroke-width:2px
    classDef piiText fill:#FFE0B2,stroke:#EF6C00,stroke-width:2px
    classDef piiFields fill:#D1C4E9,stroke:#5E35B1,stroke-width:2px
    classDef control fill:#ECEFF1,stroke:#455A64,stroke-width:2px
    classDef normal fill:#E3F2FD,stroke:#1976D2,stroke-width:2px
```

*Note:* PII exists in memory within Web/API/OCR Worker during processing.  
The **logs/metrics sink must be non-sensitive** and contain **no OCR text and no field values**.

---

## Privacy guardrails (enforced)
- **No image persistence** by default in API or OCR worker
- **No PII logs** (no OCR text, no parsed fields, no images)
- API responses include: `Cache-Control: no-store`
- HTTPS in staging
- OCR worker protected with `X-INTERNAL-KEY` (recommended)

---

## Safe logging examples

✅ Allowed log line:
- `requestId=... selectedEngine=paddle confidence=0.82 processingMs=920 outcome=SUCCESS`

❌ Forbidden log lines:
- Any line containing:
  - raw OCR text
  - name/DOB/address/licence number
  - image metadata that can identify a person

---

## Security notes (POC level)
- The OCR worker should not be publicly reachable if deployment allows private services.
- If public exposure is required for staging, add:
  - internal auth header
  - basic rate limiting at API level
  - request size limits
