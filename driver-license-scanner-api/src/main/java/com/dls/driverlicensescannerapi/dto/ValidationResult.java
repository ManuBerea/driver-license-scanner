package com.dls.driverlicensescannerapi.dto;

import java.util.List;

public record ValidationResult(List<ValidationError> blockingErrors, List<String> warnings) {}
