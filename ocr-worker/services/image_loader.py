from dataclasses import dataclass
from io import BytesIO

from fastapi import status
from PIL import Image, UnidentifiedImageError


@dataclass(frozen=True)
class ImageValidationError(Exception):
    code: str
    message: str
    status_code: int


def load_image(image_bytes: bytes, max_bytes: int) -> Image.Image:
    if not image_bytes:
        raise ImageValidationError(
            "INVALID_IMAGE",
            "Image could not be processed.",
            status.HTTP_400_BAD_REQUEST,
        )

    if len(image_bytes) > max_bytes:
        raise ImageValidationError(
            "IMAGE_TOO_LARGE",
            "Image is too large.",
            status.HTTP_413_CONTENT_TOO_LARGE,
        )

    try:
        image = Image.open(BytesIO(image_bytes))
        image.load()
        return image.convert("RGB")
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError):
        raise ImageValidationError(
            "INVALID_IMAGE",
            "Image could not be processed.",
            status.HTTP_400_BAD_REQUEST,
        )
