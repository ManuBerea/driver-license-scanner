package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.ocr.OcrLine;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import com.dls.driverlicensescannerapi.ocr.OcrClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    @Mock
    private OcrClient ocrClient;

    @Mock
    private ResponseAssembler responseAssembler;

    @InjectMocks
    private ScanService scanService;

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

        scanService.scan(image, "req-1");

        verify(responseAssembler).assemble(any(), any(), fieldsCaptor.capture());
        LicenseFields parsed = fieldsCaptor.getValue();

        assertEquals("ANDREA", parsed.firstName());
        assertEquals("CAMPBELL", parsed.lastName());
        assertEquals("05.07.1964", parsed.dateOfBirth());
        assertEquals("30.11.2031", parsed.expiryDate());
        assertEquals("99999999", parsed.licenceNumber());
    }
}
