from fastapi import FastAPI, File
from pydantic import BaseModel
from uuid import uuid4

def create_app() -> FastAPI:
    app = FastAPI(title="Driver License Scanner", version="0.1.0")

    class OcrResponse(BaseModel):
        requestId: str
        engine: str
        confidence: float
        lines: list[str]
        processingTimeMs: int

    @app.get("/health")
    async def health() -> dict:
        return {"status": "ok"}

    @app.post("/ocr", response_model=OcrResponse)
    async def ocr(image: bytes = File(...)) -> OcrResponse:
        _ = image
        return OcrResponse(
            requestId=str(uuid4()),
            engine="stub",
            confidence=0.0,
            lines=[],
            processingTimeMs=1,
        )

    return app

application = create_app()
