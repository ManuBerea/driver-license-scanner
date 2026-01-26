package com.dls.driverlicensescannerapi.controller;

import com.dls.driverlicensescannerapi.dto.ErrorDetail;
import com.dls.driverlicensescannerapi.dto.ErrorResponse;
import com.dls.driverlicensescannerapi.error.ErrorCatalog;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handlePayloadTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                request,
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCatalog.INVALID_IMAGE_CODE,
                ErrorCatalog.IMAGE_TOO_LARGE_MESSAGE
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                request,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorCatalog.INVALID_IMAGE_CODE,
                ErrorCatalog.UNSUPPORTED_MEDIA_TYPE_MESSAGE
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartError(
            MultipartException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                request,
                HttpStatus.BAD_REQUEST,
                ErrorCatalog.INVALID_IMAGE_CODE,
                ErrorCatalog.INVALID_FORMAT_MESSAGE
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                request,
                HttpStatus.BAD_REQUEST,
                ErrorCatalog.INVALID_IMAGE_CODE,
                ErrorCatalog.MISSING_IMAGE_MESSAGE
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message
    ) {
        String requestId = resolveRequestId(request);
        ErrorResponse response = new ErrorResponse(
                requestId,
                new ErrorDetail(code, message)
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
