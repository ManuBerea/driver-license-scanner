package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ValidationResult;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationServiceTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ValidationService validationService = new ValidationService();

    @Test
    void missingRequiredFieldsAreBlocking() {
        LicenseFields fields = new LicenseFields(null, null, null, null, null, null, List.of());

        ValidationResult result = validationService.validate(fields);

        assertEquals(6, result.blockingErrors().size());
        assertTrue(result.blockingErrors().stream()
                .allMatch(error -> "MISSING_REQUIRED_FIELD".equals(error.code())));
    }

    @Test
    void invalidPostcodeIsBlocking() {
        LicenseFields fields = validFields()
                .withAddressLine("123 CASTLEROCK ROAD, COLERAINE")
                .build();

        ValidationResult result = validationService.validate(fields);

        assertTrue(result.blockingErrors().stream()
                .anyMatch(error -> "INVALID_POSTCODE".equals(error.code())));
    }

    @Test
    void invalidLicenceNumberIsBlocking() {
        LicenseFields fields = validFields()
                .withLicenceNumber("INVALID123")
                .build();

        ValidationResult result = validationService.validate(fields);

        assertTrue(result.blockingErrors().stream()
                .anyMatch(error -> "INVALID_LICENCE_NUMBER".equals(error.code())));
    }

    @Test
    void expiryDateInPastIsBlocking() {
        String expired = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
        LicenseFields fields = validFields()
                .withExpiryDate(expired)
                .build();

        ValidationResult result = validationService.validate(fields);

        assertTrue(result.blockingErrors().stream()
                .anyMatch(error -> "EXPIRY_DATE_PAST".equals(error.code())));
    }

    @Test
    void ageOutsideRangeIsWarning() {
        String tooYoung = LocalDate.now().minusYears(20).format(DATE_FORMATTER);
        LicenseFields fields = validFields()
                .withDateOfBirth(tooYoung)
                .build();

        ValidationResult result = validationService.validate(fields);

        assertEquals(1, result.warnings().size());
        assertEquals("Age outside 21-75", result.warnings().get(0));
        assertTrue(result.blockingErrors().isEmpty());
    }

    private LicenseFieldsBuilder validFields() {
        String dob = LocalDate.now().minusYears(30).format(DATE_FORMATTER);
        String expiry = LocalDate.now().plusYears(1).format(DATE_FORMATTER);
        return new LicenseFieldsBuilder()
                .withFirstName("ANDREA")
                .withLastName("CAMPBELL")
                .withDateOfBirth(dob)
                .withAddressLine("123 CASTLEROCK ROAD, COLERAINE, BT51 3TB")
                .withLicenceNumber("SMITH801201AB1CD")
                .withExpiryDate(expiry);
    }

    private static final class LicenseFieldsBuilder {
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String addressLine;
        private String licenceNumber;
        private String expiryDate;

        LicenseFieldsBuilder withFirstName(String value) {
            this.firstName = value;
            return this;
        }

        LicenseFieldsBuilder withLastName(String value) {
            this.lastName = value;
            return this;
        }

        LicenseFieldsBuilder withDateOfBirth(String value) {
            this.dateOfBirth = value;
            return this;
        }

        LicenseFieldsBuilder withAddressLine(String value) {
            this.addressLine = value;
            return this;
        }

        LicenseFieldsBuilder withLicenceNumber(String value) {
            this.licenceNumber = value;
            return this;
        }

        LicenseFieldsBuilder withExpiryDate(String value) {
            this.expiryDate = value;
            return this;
        }

        LicenseFields build() {
            return new LicenseFields(
                    firstName,
                    lastName,
                    dateOfBirth,
                    addressLine,
                    licenceNumber,
                    expiryDate,
                    List.of()
            );
        }
    }
}
