package com.dls.driverlicensescannerapi.controller;

import com.dls.driverlicensescannerapi.dto.ErrorDetail;
import com.dls.driverlicensescannerapi.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String INVALID_IMAGE_CODE = "INVALID_IMAGE";
    private static final String INVALID_IMAGE_MESSAGE =
            "Invalid image. Please upload a JPG, PNG, or WEBP file under 10MB.";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handlePayloadTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(request, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpServletRequest request, HttpStatus status) {
        String requestId = resolveRequestId(request);
        ErrorResponse response = new ErrorResponse(
                requestId,
                new ErrorDetail(INVALID_IMAGE_CODE, INVALID_IMAGE_MESSAGE)
        );
        return ResponseEntity.status(status)
                .headers(noStoreHeaders())
                .body(response);
    }

    private HttpHeaders noStoreHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        return headers;
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
