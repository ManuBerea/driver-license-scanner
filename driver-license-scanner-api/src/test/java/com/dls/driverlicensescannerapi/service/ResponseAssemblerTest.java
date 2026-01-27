package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseAssemblerTest {

    @Test
    void computesConfidenceFromMissingFields() {
        ValidationService validationService = new ValidationService();
        ScanResponse response = getScanResponse(validationService);

        assertEquals(0.70, response.ocrConfidence());
        assertEquals(0.70, response.confidenceThreshold());
        assertTrue(response.validation().blockingErrors().stream()
                .anyMatch(error -> "MISSING_REQUIRED_FIELD".equals(error.code())));
    }

    private static ScanResponse getScanResponse(ValidationService validationService) {
        ResponseAssembler assembler = new ResponseAssembler(0.70, validationService);

        LicenseFields fields = new LicenseFields(
                "ANDREA",
                null,
                "05.07.1964",
                null,
                "SMITH801201AB1CD",
                "30.11.2031",
                List.of()
        );

        OcrResult ocrResult = new OcrResult("req-1", "paddle", 0.9, List.of(), 123L, null);

        ScanResponse response = assembler.assemble("req-1", ocrResult, fields);
        return response;
    }
}
