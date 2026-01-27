package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ValidationError;
import com.dls.driverlicensescannerapi.dto.ValidationResult;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ValidationService {

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseStrict()
            .appendPattern("dd.MM.uuuu")
            .toFormatter(Locale.UK);

    private static final Pattern POSTCODE_PATTERN =
            Pattern.compile("\\b[A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern LICENCE_NUMBER_PATTERN =
            Pattern.compile("^[A-Z]{5}\\d{6}[A-Z]{2}\\d[A-Z]{2}\\d{0,2}$");

    public ValidationResult validate(LicenseFields fields) {
        List<ValidationError> blockingErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (fields == null) {
            addMissingRequiredBlocking(null, blockingErrors);
            return new ValidationResult(blockingErrors, warnings);
        }

        addMissingRequiredBlocking(fields, blockingErrors);
        addExpiryValidation(fields, blockingErrors);
        addPostcodeBlocking(fields, blockingErrors);
        addLicenceNumberBlocking(fields, blockingErrors);
        addAgeWarning(fields, warnings);

        return new ValidationResult(blockingErrors, warnings);
    }

    private void addMissingRequiredBlocking(LicenseFields fields, List<ValidationError> blockingErrors) {
        if (fields == null) {
            addMissingRequired("firstName", blockingErrors);
            addMissingRequired("lastName", blockingErrors);
            addMissingRequired("dateOfBirth", blockingErrors);
            addMissingRequired("addressLine", blockingErrors);
            addMissingRequired("licenceNumber", blockingErrors);
            addMissingRequired("expiryDate", blockingErrors);
            return;
        }
        addMissingRequiredIfBlank(fields.firstName(), "firstName", blockingErrors);
        addMissingRequiredIfBlank(fields.lastName(), "lastName", blockingErrors);
        addMissingRequiredIfBlank(fields.dateOfBirth(), "dateOfBirth", blockingErrors);
        addMissingRequiredIfBlank(fields.addressLine(), "addressLine", blockingErrors);
        addMissingRequiredIfBlank(fields.licenceNumber(), "licenceNumber", blockingErrors);
        addMissingRequiredIfBlank(fields.expiryDate(), "expiryDate", blockingErrors);
    }

    private void addMissingRequiredIfBlank(String value, String field, List<ValidationError> blockingErrors) {
        if (value == null || value.isBlank()) {
            addMissingRequired(field, blockingErrors);
        }
    }

    private void addMissingRequired(String field, List<ValidationError> blockingErrors) {
        blockingErrors.add(new ValidationError(
                "MISSING_REQUIRED_FIELD",
                field,
                "Missing required field: " + field
        ));
    }

    private void addExpiryValidation(LicenseFields fields, List<ValidationError> blockingErrors) {
        LocalDate expiryDate = parseDate(fields.expiryDate());
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            blockingErrors.add(new ValidationError(
                    "EXPIRY_DATE_PAST",
                    "expiryDate",
                    "Expiry date is in the past."
            ));
        }
    }

    private void addPostcodeBlocking(LicenseFields fields, List<ValidationError> blockingErrors) {
        String addressLine = fields.addressLine();
        if (addressLine == null || addressLine.isBlank()) {
            return;
        }
        if (!POSTCODE_PATTERN.matcher(addressLine).find()) {
            blockingErrors.add(new ValidationError(
                    "INVALID_POSTCODE",
                    "addressLine",
                    "Invalid UK postcode in addressLine."
            ));
        }
    }

    private void addLicenceNumberBlocking(LicenseFields fields, List<ValidationError> blockingErrors) {
        String licenceNumber = fields.licenceNumber();
        if (licenceNumber == null || licenceNumber.isBlank()) {
            return;
        }
        String normalized = licenceNumber.replaceAll("\\s+", "").toUpperCase(Locale.UK);
        if (!LICENCE_NUMBER_PATTERN.matcher(normalized).matches()) {
            blockingErrors.add(new ValidationError(
                    "INVALID_LICENCE_NUMBER",
                    "licenceNumber",
                    "Invalid licence number."
            ));
        }
    }

    private void addAgeWarning(LicenseFields fields, List<String> warnings) {
        LocalDate dateOfBirth = parseDate(fields.dateOfBirth());
        if (dateOfBirth == null) {
            return;
        }
        int years = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (years < 21 || years > 75) {
            warnings.add("Age outside 21-75");
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
