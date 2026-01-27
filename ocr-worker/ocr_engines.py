from __future__ import annotations

from dataclasses import dataclass
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
        return DisabledOcrEngine("vision", "Vision OCR is not configured.")

    raise OcrEngineError("OCR_FAILED", f"Unsupported OCR engine: {engine_name}", status_code=400)
