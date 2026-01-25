package com.dls.driverlicensescannerapi.controller;

import com.dls.driverlicensescannerapi.model.DriverFields;
import com.dls.driverlicensescannerapi.model.ErrorCode;
import com.dls.driverlicensescannerapi.model.ErrorDetail;
import com.dls.driverlicensescannerapi.model.ErrorResponse;
import com.dls.driverlicensescannerapi.model.ScanResponse;
import com.dls.driverlicensescannerapi.model.ValidationResult;
import com.dls.driverlicensescannerapi.service.ImageValidationService;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/license")
public class ScanController {

    private final ImageValidationService imageValidationService;

    public ScanController(ImageValidationService imageValidationService) {
        this.imageValidationService = imageValidationService;
    }

    @PostMapping(path = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> scan(
            @RequestHeader(name = "X-Request-Id", required = false) String requestId,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        String resolvedRequestId = resolveRequestId(requestId);

        Optional<String> validationError = imageValidationService.validate(image);
        if (validationError.isPresent()) {
            return invalidImage(resolvedRequestId, validationError.get());
        }

        ScanResponse response = ScanResponse.builder()
                .requestId(resolvedRequestId)
                .selectedEngine("stub")
                .attemptedEngines(Collections.singletonList("stub"))
                .ocrConfidence(0.0)
                .processingTimeMs(0L)
                .fields(new DriverFields())
                .validation(ValidationResult.builder().build())
                .build();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    private ResponseEntity<ErrorResponse> invalidImage(String requestId, String message) {
        ErrorResponse payload = ErrorResponse.builder()
                .requestId(requestId)
                .error(ErrorDetail.builder()
                        .code(ErrorCode.INVALID_IMAGE)
                        .message(message)
                        .build())
                .build();

        return ResponseEntity.badRequest()
                .cacheControl(CacheControl.noStore())
                .body(payload);
    }

    private String resolveRequestId(String suppliedRequestId) {
        String trimmed = StringUtils.trimWhitespace(suppliedRequestId);
        if (StringUtils.hasText(trimmed)) {
            return trimmed;
        }
        return UUID.randomUUID().toString();
    }
}
