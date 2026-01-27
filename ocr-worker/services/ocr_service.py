import os
import time

from core.models import OcrLineResponse, OcrResponse
from core.settings import env_bool, get_engine, max_image_bytes
from ocr_engines import OcrLine
from services.image_loader import load_image


def compute_confidence(lines: list[OcrLine]) -> float:
    if not lines:
        return 0.0
    total = sum(max(0.0, min(1.0, line.confidence)) for line in lines)
    return total / len(lines)


def run_ocr(request_id: str, image_bytes: bytes) -> OcrResponse:
    image = load_image(image_bytes, max_image_bytes())
    enable_raw_text = env_bool(os.getenv("ENABLE_OCR_RAW_TEXT"), False)

    engine = get_engine()
    start = time.perf_counter()
    lines = engine.run(image)
    elapsed_ms = int((time.perf_counter() - start) * 1000)
    confidence = compute_confidence(lines)

    raw_text = "\n".join(line.text for line in lines) if enable_raw_text else None
    response_lines = [OcrLineResponse(text=line.text, confidence=line.confidence) for line in lines]

    return OcrResponse(
        requestId=request_id,
        engine=engine.name,
        confidence=confidence,
        lines=response_lines,
        processingTimeMs=elapsed_ms,
        rawText=raw_text,
    )
