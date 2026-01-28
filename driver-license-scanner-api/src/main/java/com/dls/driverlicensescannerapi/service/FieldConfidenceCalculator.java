package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;

public final class FieldConfidenceCalculator {

    private FieldConfidenceCalculator() {}

    public static double compute(LicenseFields fields) {
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

    public static boolean hasMissingRequired(LicenseFields fields) {
        if (fields == null) {
            return true;
        }
        return isBlank(fields.firstName())
                || isBlank(fields.lastName())
                || isBlank(fields.dateOfBirth())
                || isBlank(fields.addressLine())
                || isBlank(fields.licenceNumber())
                || isBlank(fields.expiryDate());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
