package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResponseAssembler {

    private final double confidenceThreshold;
    private final ValidationService validationService;

    public ResponseAssembler(
            @Value("${OCR_CONFIDENCE_WARN_THRESHOLD:0.70}") double confidenceThreshold,
            ValidationService validationService
    ) {
        this.confidenceThreshold = confidenceThreshold;
        this.validationService = validationService;
    }

    public ScanResponse assemble(String requestId, OcrResult ocrResult, LicenseFields fields) {
        String selectedEngine = ocrResult.engine();
        List<String> attemptedEngines = selectedEngine == null ? List.of() : List.of(selectedEngine);
        double computedConfidence = computeFieldConfidence(fields);

        return new ScanResponse(
                requestId,
                selectedEngine,
                attemptedEngines,
                computedConfidence,
                confidenceThreshold,
                ocrResult.processingTimeMs(),
                fields,
                validationService.validate(fields)
        );
    }

    private double computeFieldConfidence(LicenseFields fields) {
        if (fields == null) {
            return 0.0;
        }
        int missing = 0;
        missing += isBlank(fields.firstName()) ? 1 : 0;
        missing += isBlank(fields.lastName()) ? 1 : 0;
        missing += isBlank(fields.dateOfBirth()) ? 1 : 0;
        missing += isBlank(fields.addressLine()) ? 1 : 0;
        missing += isBlank(fields.licenceNumber()) ? 1 : 0;
        missing += isBlank(fields.expiryDate()) ? 1 : 0;
        return switch (missing) {
            case 0 -> 1.0;
            case 1 -> 0.85;
            case 2 -> 0.70;
            case 3 -> 0.55;
            case 4 -> 0.40;
            case 5 -> 0.25;
            case 6 -> 0.10;
            default -> 0.0;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
