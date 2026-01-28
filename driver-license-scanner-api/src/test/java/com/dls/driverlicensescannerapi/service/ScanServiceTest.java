package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.ocr.OcrLine;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import com.dls.driverlicensescannerapi.ocr.OcrClient;
import java.util.List;

import com.dls.driverlicensescannerapi.validator.ValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    @Mock
    private OcrClient ocrClient;

    @Mock
    private ResponseAssembler responseAssembler;

    @Captor
    private ArgumentCaptor<LicenseFields> fieldsCaptor;

    @Test
    void scansWithParsedFields() {
        List<OcrLine> lines = List.of(
                new OcrLine("1. CAMPBELL", 0.99),
                new OcrLine("2. ANDREA", 0.99),
                new OcrLine("3. 05.07.1964", 0.98),
                new OcrLine("4b. 30.11.2031", 0.97),
                new OcrLine("5. 99999999", 0.99)
        );
        OcrResult ocrResult = new OcrResult("req-1", "paddle", 0.9, lines, 123L, null);
        ScanResponse response = new ScanResponse(
                "req-1",
                "paddle",
                List.of("paddle"),
                1.0,
                0.70,
                123L,
                new LicenseFields("ANDREA", "CAMPBELL", "05.07.1964", null, "99999999", "30.11.2031", List.of()),
                null
        );

        when(ocrClient.scan(any(), any())).thenReturn(ocrResult);
        when(responseAssembler.assemble(any(), any(), any())).thenReturn(response);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "license.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        ScanService scanService = new ScanService(ocrClient, responseAssembler, false, 1, 0.70);
        scanService.scan(image, "req-1");

        verify(responseAssembler).assemble(any(), any(), fieldsCaptor.capture());
        LicenseFields parsed = fieldsCaptor.getValue();

        assertEquals("ANDREA", parsed.firstName());
        assertEquals("CAMPBELL", parsed.lastName());
        assertEquals("05.07.1964", parsed.dateOfBirth());
        assertEquals("30.11.2031", parsed.expiryDate());
        assertEquals("99999999", parsed.licenceNumber());
    }

    @Test
    void fallsBackWhenConfidenceIsLow() {
        ResponseAssembler assembler = new ResponseAssembler(0.70, new ValidationService());
        ScanService service = new ScanService(ocrClient, assembler, true, 2, 0.70);

        OcrResult firstAttempt = new OcrResult("req-2", "paddle", 0.2, List.of(), 100L, null);
        OcrResult secondAttempt = new OcrResult(
                "req-2",
                "vision",
                0.9,
                List.of(
                        new OcrLine("1. CAMPBELL", 0.99),
                        new OcrLine("2. ANDREA", 0.99),
                        new OcrLine("3. 05.07.1964", 0.98),
                        new OcrLine("4b. 30.11.2031", 0.97),
                        new OcrLine("5. 99999999", 0.99),
                        new OcrLine("8. 123 CASTLEROCK ROAD, COLERAINE, BT51 3TB", 0.95)
                ),
                120L,
                null
        );

        when(ocrClient.scan(any(), any(), eq("paddle"))).thenReturn(firstAttempt);
        when(ocrClient.scan(any(), any(), eq("vision"))).thenReturn(secondAttempt);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "license.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        ScanResponse response = service.scan(image, "req-2");

        assertEquals("vision", response.selectedEngine());
        assertEquals(List.of("paddle", "vision"), response.attemptedEngines());
        verify(ocrClient, times(1)).scan(any(), any(), eq("paddle"));
        verify(ocrClient, times(1)).scan(any(), any(), eq("vision"));
    }
}
