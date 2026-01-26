package com.dls.driverlicensescannerapi.dto;

import java.util.List;

public record LicenseFields(
        String firstName,
        String lastName,
        String dateOfBirth,
        String addressLine,
        String postcode,
        String licenceNumber,
        String expiryDate,
        List<String> categories
) {}
