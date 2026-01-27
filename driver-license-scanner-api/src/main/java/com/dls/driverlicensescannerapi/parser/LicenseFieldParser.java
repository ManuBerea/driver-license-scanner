package com.dls.driverlicensescannerapi.parser;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.ocr.OcrLine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LicenseFieldParser {

    private static final Pattern LABEL_PATTERN =
            Pattern.compile("^(?<label>(?:1|2|3|4a|4b|5|7|8|9))(?=\\s|[\\.)])\\s*[\\.)]?\\s*(?<value>.*)$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{2})[./-](\\d{2})[./-](\\d{4})(?!\\d)");
    private static final Pattern POSTCODE_PATTERN =
            Pattern.compile("([A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2})", Pattern.CASE_INSENSITIVE);

    private LicenseFieldParser() {}

    public static LicenseFields parse(List<OcrLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return emptyFields();
        }

        List<String> normalizedLines = new ArrayList<>();
        for (OcrLine line : lines) {
            if (line == null || line.text() == null) {
                continue;
            }
            String normalized = normalize(line.text());
            if (!normalized.isBlank()) {
                normalizedLines.add(normalized);
            }
        }

        if (normalizedLines.isEmpty()) {
            return emptyFields();
        }

        LabelIndex labelIndex = LabelIndex.from(normalizedLines);

        String lastName = nullIfBlank(labelIndex.valueFor("1").orElse(null));
        String firstName = nullIfBlank(labelIndex.valueFor("2").orElse(null));
        if (firstName == null) {
            firstName = extractNamesFromLabel("2", normalizedLines, labelIndex).orElse(null);
        }
        if (lastName == null) {
            lastName = fallbackLastName(normalizedLines, labelIndex).orElse(null);
        }

        String dateOfBirth = extractDate(labelIndex.valueFor("3"));
        if (dateOfBirth == null) {
            dateOfBirth = extractDateFromRange(labelIndex.labelRange("2", normalizedLines));
        }
        String expiryDate = extractDate(labelIndex.valueFor("4b"));
        String licenceNumber = normalizeLicenseNumber(labelIndex.valueFor("5"));
        if (licenceNumber == null) {
            licenceNumber = normalizeLicenseNumberFromRange(labelIndex.labelRange("5", normalizedLines));
        }

        AddressResult address = extractAddress(normalizedLines, labelIndex);
        List<String> categories = extractCategories(labelIndex.valueFor("9"));
        if (categories.isEmpty()) {
            categories = extractCategoriesFromRange(labelIndex.labelRange("9", normalizedLines));
        }

        return new LicenseFields(
                nullIfBlank(firstName),
                nullIfBlank(lastName),
                dateOfBirth,
                nullIfBlank(address.addressLine()),
                nullIfBlank(address.postcode()),
                nullIfBlank(licenceNumber),
                expiryDate,
                categories
        );
    }

    private static LicenseFields emptyFields() {
        return new LicenseFields(null, null, null, null, null, null, null, List.of());
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String normalizeLicenseNumber(Optional<String> value) {
        if (value.isEmpty()) {
            return null;
        }
        String cleaned = value.get().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return nullIfBlank(cleaned);
    }

    private static String normalizeLicenseNumberFromRange(List<String> range) {
        if (range.isEmpty()) {
            return null;
        }
        String combined = normalize(String.join("", range));
        String cleaned = combined.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return nullIfBlank(cleaned);
    }

    private static String extractDate(Optional<String> value) {
        if (value.isEmpty()) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(value.get());
        if (!matcher.find()) {
            return null;
        }
        String day = matcher.group(1);
        String month = matcher.group(2);
        String year = matcher.group(3);
        return year + "-" + month + "-" + day;
    }

    private static String extractDateFromRange(List<String> range) {
        for (String line : range) {
            String date = extractDate(Optional.ofNullable(line));
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private static Optional<String> extractNamesFromLabel(
            String label,
            List<String> lines,
            LabelIndex labelIndex
    ) {
        List<String> range = labelIndex.labelRange(label, lines);
        if (range.isEmpty()) {
            return Optional.empty();
        }
        List<String> parts = new ArrayList<>();
        for (String line : range) {
            if (DATE_PATTERN.matcher(line).find()) {
                break;
            }
            parts.add(line);
        }
        String combined = normalize(String.join(" ", parts));
        return combined.isBlank() ? Optional.empty() : Optional.of(combined);
    }

    private static Optional<String> fallbackLastName(List<String> lines, LabelIndex labelIndex) {
        Optional<LabelMatch> labelTwo = labelIndex.labelMatch("2");
        if (labelTwo.isEmpty()) {
            return Optional.empty();
        }
        int index = labelTwo.get().index() - 1;
        while (index >= 0) {
            String line = lines.get(index);
            if (!LabelIndex.isLabelLine(line)) {
                String normalized = normalize(line);
                return normalized.isBlank() ? Optional.empty() : Optional.of(normalized);
            }
            index--;
        }
        return Optional.empty();
    }

    private static AddressResult extractAddress(List<String> lines, LabelIndex labelIndex) {
        Optional<LabelMatch> addressLabel = labelIndex.labelMatch("8");
        if (addressLabel.isEmpty()) {
            return new AddressResult(null, null);
        }

        int startIndex = addressLabel.get().index();
        int endIndex = labelIndex.nextLabelIndexAfter(startIndex).orElse(lines.size());

        List<String> addressParts = new ArrayList<>();
        if (!addressLabel.get().value().isBlank()) {
            addressParts.add(addressLabel.get().value());
        }
        for (int i = startIndex + 1; i < endIndex; i++) {
            String line = lines.get(i);
            if (!line.isBlank()) {
                addressParts.add(line);
            }
        }

        String combined = normalize(String.join(", ", addressParts));
        if (combined.isBlank()) {
            return new AddressResult(null, null);
        }

        Matcher matcher = POSTCODE_PATTERN.matcher(combined);
        String postcode = null;
        if (matcher.find()) {
            postcode = formatPostcode(matcher.group(1));
            combined = normalize(matcher.replaceAll("").replaceAll("[, ]+$", ""));
        }

        return new AddressResult(nullIfBlank(combined), nullIfBlank(postcode));
    }

    private static List<String> extractCategories(Optional<String> value) {
        if (value.isEmpty()) {
            return List.of();
        }
        String cleaned = value.get()
                .replaceAll("\\s+", "")
                .replaceAll("[^A-Za-z0-9/]", "");
        if (cleaned.isBlank()) {
            return List.of();
        }
        String[] tokens = cleaned.split("/");
        List<String> categories = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank()) {
                categories.add(token.toUpperCase(Locale.ROOT));
            }
        }
        return categories.isEmpty() ? List.of() : Collections.unmodifiableList(categories);
    }

    private static List<String> extractCategoriesFromRange(List<String> range) {
        if (range.isEmpty()) {
            return List.of();
        }
        String combined = normalize(String.join(" ", range));
        return extractCategories(Optional.of(combined));
    }

    private static String formatPostcode(String raw) {
        if (raw == null) {
            return null;
        }
        String upper = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (upper.length() <= 3) {
            return upper;
        }
        String outward = upper.substring(0, upper.length() - 3);
        String inward = upper.substring(upper.length() - 3);
        return (outward + " " + inward).trim();
    }

    private record AddressResult(String addressLine, String postcode) {}

    private record LabelMatch(String label, String value, int index) {}

    private static final class LabelIndex {
        private final List<LabelMatch> matches;

        private LabelIndex(List<LabelMatch> matches) {
            this.matches = matches;
        }

        static LabelIndex from(List<String> lines) {
            List<LabelMatch> matches = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher matcher = LABEL_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String label = matcher.group("label").toLowerCase(Locale.ROOT);
                    String value = normalize(matcher.group("value"));
                    matches.add(new LabelMatch(label, value, i));
                }
            }
            return new LabelIndex(matches);
        }

        Optional<String> valueFor(String label) {
            return matches.stream()
                    .filter(match -> match.label().equals(label))
                    .findFirst()
                    .map(LabelMatch::value);
        }

        Optional<LabelMatch> labelMatch(String label) {
            return matches.stream()
                    .filter(match -> match.label().equals(label))
                    .findFirst();
        }

        Optional<Integer> nextLabelIndexAfter(int index) {
            return matches.stream()
                    .map(LabelMatch::index)
                    .filter(matchIndex -> matchIndex > index)
                    .sorted()
                    .findFirst();
        }

        List<String> labelRange(String label, List<String> lines) {
            Optional<LabelMatch> match = labelMatch(label);
            if (match.isEmpty()) {
                return List.of();
            }
            int startIndex = match.get().index();
            int endIndex = nextLabelIndexAfter(startIndex).orElse(lines.size());
            List<String> range = new ArrayList<>();
            if (!match.get().value().isBlank()) {
                range.add(match.get().value());
            }
            for (int i = startIndex + 1; i < endIndex; i++) {
                String line = lines.get(i);
                if (!line.isBlank() && !isLabelLine(line)) {
                    range.add(line);
                }
            }
            return range;
        }

        static boolean isLabelLine(String line) {
            return LABEL_PATTERN.matcher(line).matches();
        }
    }
}
