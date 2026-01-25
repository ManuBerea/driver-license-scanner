package com.dls.driverlicensescannerapi.model;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResponse {
    private String requestId;
    private String selectedEngine;
    @Builder.Default
    private List<String> attemptedEngines = Collections.emptyList();
    private Double ocrConfidence;
    private Long processingTimeMs;
    private DriverFields fields;
    private ValidationResult validation;
}
