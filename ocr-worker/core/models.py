from pydantic import BaseModel


class ErrorDetail(BaseModel):
    code: str
    message: str


class ErrorResponse(BaseModel):
    requestId: str
    error: ErrorDetail


class OcrLineResponse(BaseModel):
    text: str
    confidence: float


class OcrResponse(BaseModel):
    requestId: str
    engine: str
    confidence: float
    lines: list[OcrLineResponse]
    processingTimeMs: int
    rawText: str | None = None
