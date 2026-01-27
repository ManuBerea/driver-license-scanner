package com.dls.driverlicensescannerapi.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.ocr.OcrLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class LicenseFieldParserTest {

    @Test
    void parsesLabeledFieldsDeterministically() {
        List<OcrLine> lines = List.of(
                new OcrLine("1. CAMPBELL", 0.99),
                new OcrLine("2. ANDREA JOAN", 0.99),
                new OcrLine("3. 05.07.1964 BELFAST", 0.98),
                new OcrLine("4b. 30.11.2031", 0.97),
                new OcrLine("5. 99999999", 0.99),
                new OcrLine("8. 123 CASTLEROCK ROAD, COLERAINE", 0.93),
                new OcrLine("CO. LONDONDERRY", 0.98),
                new OcrLine("BT51 3TB", 0.99),
                new OcrLine("9. AM/B1/E", 0.82)
        );

        LicenseFields fields = LicenseFieldParser.parse(lines);

        assertEquals("ANDREA JOAN", fields.firstName());
        assertEquals("CAMPBELL", fields.lastName());
        assertEquals("05.07.1964", fields.dateOfBirth());
        assertEquals("30.11.2031", fields.expiryDate());
        assertEquals("99999999", fields.licenceNumber());
        assertEquals("123 CASTLEROCK ROAD, COLERAINE CO. LONDONDERRY, BT51 3TB", fields.addressLine());
        assertEquals(List.of("AM", "B1"), fields.categories());
    }

    @Test
    void returnsNullsWhenNoLabelsPresent() {
        List<OcrLine> lines = List.of(new OcrLine("DRIVING LICENCE", 0.9));

        LicenseFields fields = LicenseFieldParser.parse(lines);

        assertNull(fields.firstName());
        assertNull(fields.lastName());
        assertNull(fields.dateOfBirth());
        assertNull(fields.addressLine());
        assertNull(fields.licenceNumber());
        assertNull(fields.expiryDate());
        assertEquals(List.of(), fields.categories());
    }

    @Test
    void parsesWhenLabelsAreSplitAcrossLines() {
        List<OcrLine> lines = List.of(
                new OcrLine("CAMPBELL", 0.99),
                new OcrLine("2.", 0.98),
                new OcrLine("ANDREA", 0.98),
                new OcrLine("JOAN", 0.97),
                new OcrLine("05.07.1964BELFAST", 0.95),
                new OcrLine("4b.30.11.2031", 0.95),
                new OcrLine("5.", 0.95),
                new OcrLine("99999999", 0.95),
                new OcrLine("8.", 0.95),
                new OcrLine("123 CASTLEROCK ROAD, COLERAINE", 0.95),
                new OcrLine("E", 0.95),
                new OcrLine("CO. LONDONDERRY", 0.95),
                new OcrLine("NOV31", 0.95),
                new OcrLine("BT513TB", 0.95),
                new OcrLine("9.", 0.95),
                new OcrLine("AM/A/B1/B/f/k//n/p/q", 0.95)
        );

        LicenseFields fields = LicenseFieldParser.parse(lines);

        assertEquals("ANDREA JOAN", fields.firstName());
        assertNull(fields.lastName());
        assertNull(fields.dateOfBirth());
        assertEquals("30.11.2031", fields.expiryDate());
        assertEquals("99999999", fields.licenceNumber());
        assertEquals("123 CASTLEROCK ROAD, COLERAINE, E, CO. LONDONDERRY, NOV31, BT51 3TB",
                fields.addressLine());
        assertEquals(List.of("AM", "A", "B1", "B", "f", "k", "n", "p", "q"), fields.categories());
    }
}
