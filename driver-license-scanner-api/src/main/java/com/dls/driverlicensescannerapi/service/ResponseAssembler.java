package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import java.util.List;

import com.dls.driverlicensescannerapi.validator.ValidationService;
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
        return assemble(requestId, ocrResult, fields, attemptedEngines, selectedEngine);
    }

    public ScanResponse assemble(
            String requestId,
            OcrResult ocrResult,
            LicenseFields fields,
            List<String> attemptedEngines,
            String selectedEngine
    ) {
        double computedConfidence = FieldConfidenceCalculator.compute(fields);

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
}
