from fastapi.responses import JSONResponse

from core.models import ErrorDetail, ErrorResponse


def error_response(request_id: str, code: str, message: str, status_code: int) -> JSONResponse:
    payload = ErrorResponse(requestId=request_id, error=ErrorDetail(code=code, message=message))
    return JSONResponse(status_code=status_code, content=payload.model_dump())
