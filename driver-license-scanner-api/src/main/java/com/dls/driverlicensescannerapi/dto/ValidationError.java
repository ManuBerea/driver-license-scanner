package com.dls.driverlicensescannerapi.dto;

public record ValidationError(String code, String field, String message) {}
