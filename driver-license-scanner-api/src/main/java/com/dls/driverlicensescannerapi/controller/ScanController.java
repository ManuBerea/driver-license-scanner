package com.dls.driverlicensescannerapi.controller;

import com.dls.driverlicensescannerapi.dto.ErrorDetail;
import com.dls.driverlicensescannerapi.dto.ErrorResponse;
import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.dto.ValidationResult;
import com.dls.driverlicensescannerapi.error.ErrorCatalog;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/license")
public class ScanController {

    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(MediaType.IMAGE_JPEG_VALUE, "image/jpg", MediaType.IMAGE_PNG_VALUE, "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp");

    @PostMapping(
            path = "/scan",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> scan(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader(value = "X-Request-Id", required = false) String requestIdHeader
    ) {
        String requestId = resolveRequestId(requestIdHeader);

        if (image == null || image.isEmpty()) {
            return errorResponse(requestId, ErrorCatalog.INVALID_IMAGE_CODE, ErrorCatalog.MISSING_IMAGE_MESSAGE);
        }

        if (image.getSize() > MAX_FILE_BYTES) {
            return errorResponse(requestId, ErrorCatalog.INVALID_IMAGE_CODE, ErrorCatalog.IMAGE_TOO_LARGE_MESSAGE);
        }

        if (!hasAllowedFormat(image)) {
            return errorResponse(requestId, ErrorCatalog.INVALID_IMAGE_CODE, ErrorCatalog.INVALID_FORMAT_MESSAGE);
        }

        ScanResponse response = new ScanResponse(
                requestId,
                "stub",
                List.of("stub"),
                0.0,
                1L,
                new LicenseFields(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of()
                ),
                new ValidationResult(List.of(), List.of())
        );

        return ResponseEntity.ok()
                .headers(noStoreHeaders())
                .body(response);
    }

    private boolean hasAllowedFormat(MultipartFile image) {
        String contentType = image.getContentType();
        boolean contentTypeAllowed = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);

        String filename = image.getOriginalFilename();
        boolean extensionAllowed = false;
        if (StringUtils.hasText(filename)) {
            String lowerName = filename.toLowerCase(Locale.ROOT);
            extensionAllowed = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        }

        return contentTypeAllowed || extensionAllowed;
    }

    private ResponseEntity<ErrorResponse> errorResponse(String requestId, String code, String message) {
        ErrorResponse response = new ErrorResponse(
                requestId,
                new ErrorDetail(code, message)
        );
        return ResponseEntity.badRequest()
                .headers(noStoreHeaders())
                .body(response);
    }

    private HttpHeaders noStoreHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        return headers;
    }

    private String resolveRequestId(String requestIdHeader) {
        if (StringUtils.hasText(requestIdHeader)) {
            return requestIdHeader;
        }
        return UUID.randomUUID().toString();
    }
}
