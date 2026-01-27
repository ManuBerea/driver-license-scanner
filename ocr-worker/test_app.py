from __future__ import annotations

from io import BytesIO

from fastapi.testclient import TestClient
from PIL import Image

import main
from core import settings
from ocr_engines import OcrLine


class FakeEngine:
    name = "fake"

    def run(self, image: Image.Image):
        _ = image
        return [OcrLine(text="TEST LINE", confidence=0.9)]


def _png_bytes() -> bytes:
    image = Image.new("RGB", (10, 10), color=(255, 255, 255))
    buffer = BytesIO()
    image.save(buffer, format="PNG")
    return buffer.getvalue()


def test_health_ok():
    client = TestClient(main.create_app())
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_ocr_unauthorized(monkeypatch):
    client = TestClient(main.create_app())
    monkeypatch.setenv("X_INTERNAL_KEY", "secret")
    response = client.post("/ocr", files={"image": ("test.png", _png_bytes(), "image/png")})
    assert response.status_code == 401
    payload = response.json()
    assert payload["error"]["code"] == "UNAUTHORIZED"


def test_ocr_success(monkeypatch):
    monkeypatch.setenv("X_INTERNAL_KEY", "secret")
    monkeypatch.setattr(settings, "resolve_engine", lambda *args, **kwargs: FakeEngine())
    settings.get_engine.cache_clear()

    client = TestClient(main.create_app())
    response = client.post(
        "/ocr",
        headers={"X-INTERNAL-KEY": "secret"},
        files={"image": ("test.png", _png_bytes(), "image/png")},
    )
    assert response.status_code == 200
    payload = response.json()
    assert payload["engine"] == "fake"
    assert payload["lines"]
    assert payload["confidence"] == 0.9
    assert payload["processingTimeMs"] >= 0


def test_ocr_too_large(monkeypatch):
    monkeypatch.setenv("X_INTERNAL_KEY", "secret")
    monkeypatch.setenv("MAX_IMAGE_BYTES", "10")
    settings.get_engine.cache_clear()
    client = TestClient(main.create_app())
    response = client.post(
        "/ocr",
        headers={"X-INTERNAL-KEY": "secret"},
        files={"image": ("big.png", b"0" * 11, "image/png")},
    )
    assert response.status_code == 413
    payload = response.json()
    assert payload["error"]["code"] == "IMAGE_TOO_LARGE"


def test_ocr_missing_image(monkeypatch):
    monkeypatch.setenv("X_INTERNAL_KEY", "secret")
    client = TestClient(main.create_app())
    response = client.post("/ocr", headers={"X-INTERNAL-KEY": "secret"})
    assert response.status_code == 400
    payload = response.json()
    assert payload["error"]["code"] == "INVALID_IMAGE"
