package com.dls.driverlicensescannerapi.dto;

public record ErrorResponse(String requestId, ErrorDetail error) {}
