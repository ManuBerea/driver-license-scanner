from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO
import logging
from typing import Protocol

from PIL import Image
import numpy as np


@dataclass(frozen=True)
class OcrLine:
    text: str
    confidence: float


class OcrEngine(Protocol):
    name: str

    def run(self, image: Image.Image) -> list[OcrLine]:
        ...


class OcrEngineError(Exception):
    def __init__(self, code: str, message: str, status_code: int = 500) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


class PaddleOcrEngine:
    name = "paddle"

    def __init__(self) -> None:
        try:
            from paddleocr import PaddleOCR
        except ImportError:
            raise OcrEngineError(
                "OCR_FAILED",
                "PaddleOCR dependencies are not installed.",
                status_code=503,
            )

        self._ocr = PaddleOCR(use_angle_cls=True, lang="en")

    def run(self, image: Image.Image) -> list[OcrLine]:
        np_image = np.array(image.convert("RGB"))[:, :, ::-1]
        result = self._ocr.ocr(np_image, cls=True)
        lines: list[OcrLine] = []
        for page in result or []:
            for item in page or []:
                if len(item) < 2:
                    continue
                text, confidence = item[1]
                if not text:
                    continue
                lines.append(OcrLine(text=str(text).strip(), confidence=float(confidence)))
        return lines


class VisionOcrEngine:
    name = "vision"

    def __init__(self) -> None:
        try:
            from google.cloud import vision  # noqa: F401
        except ImportError:
            raise OcrEngineError(
                "OCR_FAILED",
                "Vision OCR dependencies are not installed.",
                status_code=503,
            )

    def run(self, image: Image.Image) -> list[OcrLine]:
        from google.cloud import vision
        logger = logging.getLogger("uvicorn.error")

        buffer = BytesIO()
        image.convert("RGB").save(buffer, format="JPEG")
        payload = vision.Image(content=buffer.getvalue())
        try:
            client = vision.ImageAnnotatorClient()
            response = client.document_text_detection(image=payload)
        except Exception as exc:
            logger.warning("vision_ocr_error type=%s", type(exc).__name__)
            raise OcrEngineError("OCR_FAILED", "Vision OCR failed.", status_code=503) from exc

        if response.error and response.error.message:
            logger.warning("vision_ocr_api_error code=%s", response.error.code)
            raise OcrEngineError(
                "OCR_FAILED",
                "Vision OCR failed.",
                status_code=503,
            )

        full_text = response.full_text_annotation.text or ""
        lines = [line.strip() for line in full_text.splitlines() if line.strip()]
        confidence = _average_vision_confidence(response)
        return [OcrLine(text=line, confidence=confidence) for line in lines]


class DisabledOcrEngine:
    def __init__(self, name: str, message: str) -> None:
        self.name = name
        self._message = message

    def run(self, image: Image.Image) -> list[OcrLine]:
        _ = image
        raise OcrEngineError("OCR_FAILED", self._message, status_code=503)


def resolve_engine(
    engine_name: str,
    enable_vision: bool,
) -> OcrEngine:
    name = (engine_name or "paddle").strip().lower()
    if name == "paddle":
        return PaddleOcrEngine()
    if name == "vision":
        if not enable_vision:
            return DisabledOcrEngine("vision", "Vision OCR is disabled.")
        return VisionOcrEngine()

    raise OcrEngineError("OCR_FAILED", f"Unsupported OCR engine: {engine_name}", status_code=400)


def _average_vision_confidence(response) -> float:
    confidences = []
    full_text = getattr(response, "full_text_annotation", None)
    if not full_text:
        return 0.0
    for page in full_text.pages or []:
        for block in page.blocks or []:
            for paragraph in block.paragraphs or []:
                for word in paragraph.words or []:
                    if word.confidence is not None:
                        confidences.append(float(word.confidence))
    if not confidences:
        return 0.0
    return sum(confidences) / len(confidences)
