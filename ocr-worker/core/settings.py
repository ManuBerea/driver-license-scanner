from functools import lru_cache
import os

from ocr_engines import OcrEngine, resolve_engine


def env_bool(value: str | None, default: bool = False) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


def max_image_bytes() -> int:
    value = os.getenv("MAX_IMAGE_BYTES")
    if value:
        try:
            parsed = int(value.strip())
            if parsed > 0:
                return parsed
        except ValueError:
            pass
    return 10 * 1024 * 1024


@lru_cache(maxsize=1)
def get_engine() -> OcrEngine:
    engine_name = os.getenv("OCR_ENGINE", "paddle")
    enable_vision = env_bool(os.getenv("ENABLE_VISION_OCR"), False)
    return resolve_engine(engine_name, enable_vision)
