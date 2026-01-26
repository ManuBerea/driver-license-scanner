package com.dls.driverlicensescannerapi.dto;

import java.util.List;

public record ScanResponse(
        String requestId,
        String selectedEngine,
        List<String> attemptedEngines,
        double ocrConfidence,
        long processingTimeMs,
        LicenseFields fields,
        ValidationResult validation
) {}
