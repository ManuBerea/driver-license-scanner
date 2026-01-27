package com.dls.driverlicensescannerapi.parser;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DateParser {
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{2})[./-](\\d{2})[./-](\\d{2,4})(?!\\d)");

    private DateParser() {}

    static String findFirstDate(List<String> lines, boolean isBirthDate) {
        for (String line : lines) {
            String date = parseDate(Optional.ofNullable(line), isBirthDate);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    static String parseDate(Optional<String> value, boolean isBirthDate) {
        if (value.isEmpty()) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(value.get());
        if (!matcher.find()) {
            return null;
        }
        String day = matcher.group(1);
        String month = matcher.group(2);
        String year = normalizeYear(matcher.group(3), isBirthDate);
        if (year == null) {
            return null;
        }
        return day + "." + month + "." + year;
    }

    private static String normalizeYear(String raw, boolean isBirthDate) {
        if (raw == null) {
            return null;
        }
        if (raw.length() == 4) {
            return raw;
        }
        if (raw.length() == 2) {
            int twoDigit = Integer.parseInt(raw);
            if (!isBirthDate) {
                return "20" + String.format("%02d", twoDigit);
            }
            int currentTwoDigit = Year.now().getValue() % 100;
            int century = twoDigit <= currentTwoDigit ? 2000 : 1900;
            return String.valueOf(century + twoDigit);
        }
        return null;
    }
}
