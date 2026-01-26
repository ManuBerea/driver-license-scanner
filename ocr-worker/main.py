from fastapi import FastAPI, File, Header, status
from fastapi.responses import JSONResponse
from uuid import uuid4
import logging
import os

from core.models import ErrorResponse, OcrResponse
from core.responses import error_response
from ocr_engines import OcrEngineError
from services.image_loader import ImageValidationError
from services.ocr_service import run_ocr

logger = logging.getLogger("ocr-worker")


def create_app() -> FastAPI:
    app = FastAPI(title="Driver License Scanner", version="0.2.0")

    @app.get("/health")
    async def health() -> dict:
        return {"status": "ok"}

    @app.post("/ocr", response_model=OcrResponse, responses={
        400: {"model": ErrorResponse},
        401: {"model": ErrorResponse},
        413: {"model": ErrorResponse},
        503: {"model": ErrorResponse},
        500: {"model": ErrorResponse},
    })
    async def ocr(
        image: bytes | None = File(default=None),
        x_internal_key: str | None = Header(default=None, alias="X-INTERNAL-KEY"),
    ) -> JSONResponse | OcrResponse:
        request_id = str(uuid4())
        expected_key = os.getenv("X_INTERNAL_KEY")
        if not expected_key or x_internal_key != expected_key:
            return error_response(
                request_id,
                "UNAUTHORIZED",
                "Unauthorized request.",
                status.HTTP_401_UNAUTHORIZED,
            )

        try:
            response = run_ocr(request_id, image or b"")
        except ImageValidationError as exc:
            return error_response(request_id, exc.code, exc.message, exc.status_code)
        except OcrEngineError as exc:
            return error_response(request_id, exc.code, exc.message, exc.status_code)
        except Exception:
            return error_response(
                request_id,
                "OCR_FAILED",
                "OCR engine failed.",
                status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        logger.info(
            "ocr_complete requestId=%s engine=%s confidence=%.3f timeMs=%d",
            request_id,
            response.engine,
            response.confidence,
            response.processingTimeMs,
        )

        return response

    return app


application = create_app()
